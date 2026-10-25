package my.carglass;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements LocationListener {
    // Home-screen tiles, used until the user changes them (package, label).
    private static final String[] DEFAULT_TILES = {
            "com.imotor.fmam", "com.google.android.youtube", "com.google.android.apps.youtube.music",
            "com.pintaz.customer", "com.imotor.btmusic", "com.imotor.phoneconnect",
    };
    private static final Map<String, String> LABELS = new HashMap<>();
    // Shown first inside "Semua apps".
    private static final String[] PINNED = {
            "com.imotor.fmam", "com.imotor.btmusic", "com.imotor.music", "com.imotor.video",
            "com.imotor.navigation", "com.imotor.phoneconnect", "com.imotor.avin", "com.imotor.aux",
    };
    // Launchable packages we never list.
    private static final String[] HIDDEN = {
            "my.carglass", "com.android.launcher3", "com.imotor.steerwheel",
            "com.imotor.help", "com.ms.ms2160",
    };
    private static final double DEFAULT_LAT = 3.139, DEFAULT_LNG = 101.687; // Kuala Lumpur, until a GPS fix
    private static final long WEATHER_EVERY_MS = 30 * 60_000L, WEATHER_STALE_MS = 3 * 3600_000L;

    static {
        LABELS.put("com.imotor.fmam", "Radio");
        LABELS.put("com.imotor.btmusic", "BT Muzik");
        LABELS.put("com.imotor.music", "Muzik");
        LABELS.put("com.imotor.video", "Video");
        LABELS.put("com.imotor.navigation", "Navigasi");
        LABELS.put("com.imotor.phoneconnect", "Carlink");
        LABELS.put("com.pintaz.customer", "Pintaz");
        LABELS.put("com.imotor.avin", "AV IN");
        LABELS.put("com.imotor.aux", "AUX");
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private final List<View> glassViews = new ArrayList<>();
    private GpsLog gpsLog;
    private float cq;

    private FrameLayout root, contentHolder, drawer;
    private BackgroundView bg;
    private RainView rainView;
    private SplashView splash;
    private TextView drawerTitle;

    // State
    private boolean light, raining;
    private float dayFactor, dayOverride = -1f;
    private double lat = DEFAULT_LAT, lng = DEFAULT_LNG;
    private Location lastFix;
    private float tripMeters, lastSpeed;
    private int satUsed = -1;
    private int[] times = new int[6];
    private int nextPrayer;
    private String nextLabel = "";
    private boolean wifiOn, internet, btOn;
    private String ssid, btName;
    private Integer wxTemp;
    private int wxCode;
    private String wxPlace;
    private long wxAt;
    private boolean fetching;
    private final String[] tiles = new String[6];
    private int pick = -1;
    private boolean gnssRegistered, hasFix;

    // Widgets (recreated by rebuild)
    private SpeedometerView speedo;
    private TextView clockTv, ampmTv, dateTv, tripTv, nextLineTv, locTv, wxIconTv, wxTempTv, wxCondTv, wxHintTv;
    private final TextView[] prayerName = new TextView[6], prayerTime = new TextView[6];
    private final LinearLayout[] prayerCell = new LinearLayout[6];
    private final TextView[] chipTv = new TextView[3];
    private final ImageView[] chipIv = new ImageView[3];

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            pollNet();
            pollBluetooth();
            maybeFetchWeather();
            update();
            handler.postDelayed(this, 15_000);
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(a)) splash.play(null);
            else {
                pollNet();
                pollBluetooth();
                updateChips();
            }
        }
    };

    private final GnssStatus.Callback gnss = new GnssStatus.Callback() {
        @Override public void onSatelliteStatusChanged(GnssStatus s) {
            int used = 0;
            for (int i = 0; i < s.getSatelliteCount(); i++) if (s.usedInFix(i)) used++;
            satUsed = used;
            updateChips();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        cq = Math.max(dm.widthPixels, dm.heightPixels) / 100f;
        prefs = getSharedPreferences("cg", MODE_PRIVATE);
        gpsLog = new GpsLog(this);
        lat = Double.longBitsToDouble(prefs.getLong("lat", Double.doubleToLongBits(DEFAULT_LAT)));
        lng = Double.longBitsToDouble(prefs.getLong("lng", Double.doubleToLongBits(DEFAULT_LNG)));
        hasFix = prefs.contains("lat");
        wxAt = prefs.getLong("wxAt", 0);
        wxCode = prefs.getInt("wxCode", 0);
        wxPlace = prefs.getString("wxPlace", null);
        // Only trust cached weather that was fetched for a real GPS position, not the default city.
        if (prefs.getBoolean("wxFix", false) && prefs.contains("wxTemp")) wxTemp = prefs.getInt("wxTemp", 0);
        raining = wxTemp != null && WeatherClient.isRain(wxCode) && System.currentTimeMillis() - wxAt < WEATHER_STALE_MS;
        loadTiles();

        root = new FrameLayout(this);
        bg = new BackgroundView(this);
        bg.setOnChange(this::repaintGlass);
        root.addView(bg);
        rainView = new RainView(this);
        rainView.setVisibility(raining ? View.VISIBLE : View.GONE);
        root.addView(rainView);
        contentHolder = new FrameLayout(this);
        root.addView(contentHolder, new FrameLayout.LayoutParams(-1, -1));
        drawer = new FrameLayout(this);
        drawer.setVisibility(View.GONE);
        root.addView(drawer, new FrameLayout.LayoutParams(-1, -1));
        splash = new SplashView(this);
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        computeTimes();
        light = dayFactor >= .5f && !raining;
        bg.setDayFactor(dayFactor);
        pollNet();
        pollBluetooth();
        rebuild();

        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_ON);
        f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        f.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        f.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, f);
        splash.play(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ theme + layout

    private int fg() {
        return light ? 0xFF0D1B2E : 0xFFFFFFFF;
    }

    private int fgSoft() {
        return light ? 0xB80D1B2E : 0xA6FFFFFF;
    }

    private int accent() {
        return light ? 0xFF0277BD : 0xFF4FC3F7;
    }

    private int okColor() {
        return light ? 0xFF1B8A4B : 0xFF69F0AE;
    }

    private int badColor() {
        return light ? 0xFFC62828 : 0xFFFF8A80;
    }

    private int px(float cqUnits) {
        return Math.round(cqUnits * cq);
    }

    private TextView text(float cqSize, int color, int weight) {
        TextView tv = new TextView(this);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, cqSize * cq);
        tv.setTextColor(color);
        tv.setTypeface(Fonts.get(this, weight));
        tv.setIncludeFontPadding(false);
        return tv;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    private Drawable glass(boolean accentFill, float radiusCq) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(accentFill ? accent() : light ? 0xBDFFFFFF : 0x17FFFFFF);
        normal.setStroke(Math.max(1, px(.1f)), accentFill ? accent() : light ? 0xE6FFFFFF : 0x29FFFFFF);
        normal.setCornerRadius(px(radiusCq));
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(accentFill ? (light ? 0xFF0288D1 : 0xFF81D4FA) : light ? 0xEBFFFFFF : 0x33FFFFFF);
        pressed.setStroke(Math.max(1, px(.15f)), accent());
        pressed.setCornerRadius(px(radiusCq));
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, pressed);
        s.addState(new int[]{android.R.attr.state_focused}, pressed);
        s.addState(new int[]{}, normal);
        return s;
    }

    /** Frosted-glass background for a view; the view is remembered so it repaints when the scene behind it changes. */
    private Drawable glassBg(View v, float radiusCq, boolean highlight, boolean circle) {
        if (!glassViews.contains(v)) glassViews.add(v);
        return new GlassDrawable(bg, v, px(radiusCq), circle, light, highlight, accent());
    }

    private void repaintGlass() {
        for (View v : glassViews) v.invalidate();
    }

    private void rebuild() {
        speedoReset();
        glassViews.clear();
        contentHolder.removeAllViews();
        contentHolder.addView(buildContent(), new FrameLayout.LayoutParams(-1, -1));
        buildDrawer();
        rainView.setVisibility(raining ? View.VISIBLE : View.GONE);
        bg.setRain(raining);
        speedo.setSpeedImmediate(lastSpeed);
        refreshTexts();
    }

    private void speedoReset() {
        // placeholder so rebuild keeps the same order whether or not a gauge exists yet
    }

    private View buildContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(px(2.6f), px(2.2f), px(2.6f), px(2.0f));

        // ---- left: weather + speedometer
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER);
        LinearLayout wx = new LinearLayout(this);
        wx.setOrientation(LinearLayout.VERTICAL);
        wx.setGravity(Gravity.CENTER_HORIZONTAL);
        locTv = text(1.9f, fgSoft(), 500);
        locTv.setGravity(Gravity.CENTER);
        wx.addView(locTv);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        wxIconTv = text(2.8f, fg(), 400);
        wxTempTv = text(3.0f, fg(), 300);
        wxCondTv = text(2.0f, fgSoft(), 500);
        row.addView(wxIconTv);
        LinearLayout.LayoutParams tl = lp(-2, -2, 0);
        tl.setMargins(px(1f), 0, px(1f), 0);
        row.addView(wxTempTv, tl);
        row.addView(wxCondTv);
        LinearLayout.LayoutParams rl = lp(-2, -2, 0);
        rl.topMargin = px(.4f);
        wx.addView(row, rl);
        wxHintTv = text(1.25f, accent(), 600);
        wxHintTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hl = lp(-2, -2, 0);
        hl.topMargin = px(.5f);
        wx.addView(wxHintTv, hl);
        left.addView(wx);
        speedo = new SpeedometerView(this);
        speedo.setTheme(light, accent());
        if (light) speedo.setBackground(glassBg(speedo, 0f, false, true));
        LinearLayout.LayoutParams sl = lp(px(30.5f), px(30.5f), 0);
        sl.topMargin = px(.8f);
        left.addView(speedo, sl);
        content.addView(left, lp(0, -1, 36f));

        // ---- right
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(px(2.4f), 0, 0, 0);
        content.addView(right, lp(0, -1, 64f));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout clockCol = new LinearLayout(this);
        clockCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setBaselineAligned(true);
        clockTv = text(5.9f, fg(), 700);
        ampmTv = text(1.8f, accent(), 700);
        ampmTv.setLetterSpacing(.06f);
        ampmTv.setSingleLine(true);
        ampmTv.setMinWidth(px(3.6f));
        clockRow.addView(clockTv);
        LinearLayout.LayoutParams al = lp(-2, -2, 0);
        al.leftMargin = px(.7f);
        clockRow.addView(ampmTv, al);
        clockCol.addView(clockRow);
        dateTv = text(1.9f, fgSoft(), 500);
        LinearLayout.LayoutParams dl = lp(-2, -2, 0);
        dl.topMargin = px(.5f);
        clockCol.addView(dateTv, dl);
        clockCol.setMinimumWidth(px(17.2f));
        top.addView(clockCol, lp(-2, -2, 0));

        LinearLayout rightTop = new LinearLayout(this);
        rightTop.setOrientation(LinearLayout.VERTICAL);
        rightTop.setGravity(Gravity.END);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(0, R.drawable.ic_wifi));
        chips.addView(chip(1, R.drawable.ic_bt));
        chips.addView(chip(2, R.drawable.ic_gps));
        rightTop.addView(chips);
        tripTv = text(1.5f, fgSoft(), 500);
        LinearLayout.LayoutParams trl = lp(-2, -2, 0);
        trl.topMargin = px(1f);
        rightTop.addView(tripTv, trl);
        LinearLayout.LayoutParams rtl = lp(0, -2, 1f);
        rtl.rightMargin = px(5.2f); // keeps the chips clear of the unit's floating button
        top.addView(rightTop, rtl);
        right.addView(top);

        LinearLayout prayerRow = new LinearLayout(this);
        for (int i = 0; i < 6; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, px(.7f), 0, px(.7f));
            prayerName[i] = text(1.3f, fgSoft(), 500);
            prayerName[i].setText(PrayerTimes.NAMES[i]);
            prayerName[i].setGravity(Gravity.CENTER);
            prayerTime[i] = text(2.0f, fg(), 600);
            prayerTime[i].setGravity(Gravity.CENTER);
            cell.addView(prayerName[i]);
            LinearLayout.LayoutParams tp = lp(-2, -2, 0);
            tp.topMargin = px(.2f);
            cell.addView(prayerTime[i], tp);
            prayerCell[i] = cell;
            LinearLayout.LayoutParams pl = lp(0, -2, 1f);
            pl.setMargins(px(.4f), 0, px(.4f), 0);
            prayerRow.addView(cell, pl);
        }
        LinearLayout.LayoutParams prl = lp(-1, -2, 0);
        prl.topMargin = px(1.6f);
        right.addView(prayerRow, prl);

        nextLineTv = text(1.4f, fgSoft(), 500);
        nextLineTv.setPadding(0, px(.6f), 0, 0);
        right.addView(nextLineTv);

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setGravity(Gravity.BOTTOM);
        TextView label = text(1.2f, light ? 0x990D1B2E : 0x80FFFFFF, 500);
        label.setLetterSpacing(.2f);
        label.setText("APLIKASI  ·  TAHAN LAMA UNTUK TUKAR");
        labelRow.addView(label, lp(0, -2, 1f));
        TextView all = text(1.55f, light ? Color.WHITE : 0xFF04121C, 700);
        all.setText("▦  Semua apps");
        all.setPadding(px(1.8f), px(.65f), px(1.8f), px(.65f));
        all.setBackground(glass(true, 3f));
        all.setClickable(true);
        all.setOnClickListener(v -> openDrawer(-1));
        labelRow.addView(all);
        LinearLayout.LayoutParams lrl = lp(-1, -2, 0);
        lrl.topMargin = px(1.2f);
        lrl.bottomMargin = px(.8f);
        right.addView(labelRow, lrl);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        right.addView(grid, lp(-1, 0, 1f));
        for (int r = 0; r < 2; r++) {
            LinearLayout rowL = new LinearLayout(this);
            for (int c = 0; c < 3; c++) {
                LinearLayout.LayoutParams cl = lp(0, -1, 1f);
                cl.setMargins(px(.6f), px(.6f), px(.6f), px(.6f));
                rowL.addView(mainTile(r * 3 + c), cl);
            }
            grid.addView(rowL, lp(-1, 0, 1f));
        }
        return content;
    }

    private LinearLayout chip(int idx, int iconRes) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(px(.8f), px(.5f), px(1.1f), px(.5f));
        c.setBackground(glassBg(c, 2f, false, false));
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        c.addView(iv, new LinearLayout.LayoutParams(px(1.7f), px(1.7f)));
        TextView tv = text(1.3f, fg(), 500);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setMaxWidth(px(idx == 0 ? 9.5f : idx == 1 ? 9f : 11f));
        LinearLayout.LayoutParams tl = lp(-2, -2, 0);
        tl.leftMargin = px(.6f);
        c.addView(tv, tl);
        chipTv[idx] = tv;
        chipIv[idx] = iv;
        c.setClickable(true);
        c.setOnClickListener(v -> openSettings(idx));
        LinearLayout.LayoutParams cl = lp(-2, -2, 0);
        cl.leftMargin = px(.8f);
        c.setLayoutParams(cl);
        return c;
    }

    private void openSettings(int idx) {
        String action = idx == 0 ? Settings.ACTION_WIFI_SETTINGS
                : idx == 1 ? Settings.ACTION_BLUETOOTH_SETTINGS : Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        try {
            startActivity(new Intent(action));
        } catch (Exception ignored) {
        }
    }

    private View mainTile(final int index) {
        PackageManager pm = getPackageManager();
        final String pkg = tiles[index];
        final Intent launch = pkg == null ? null : pm.getLaunchIntentForPackage(pkg);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setBackground(glassBg(t, 2.2f, false, false));
        t.setClickable(true);
        ImageView iv = new ImageView(this);
        String label;
        if (launch != null) {
            iv.setImageDrawable(Icons.forApp(this, pkg, launch));
            label = labelFor(pkg);
        } else {
            label = "Pilih app";
            iv.setImageResource(android.R.drawable.ic_menu_add);
            iv.setColorFilter(fgSoft(), PorterDuff.Mode.SRC_IN);
        }
        t.addView(iv, new LinearLayout.LayoutParams(px(5.4f), px(5.4f)));
        TextView tv = text(2.15f, fg(), 600);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setText(label);
        LinearLayout.LayoutParams ll = lp(-2, -2, 0);
        ll.topMargin = px(.6f);
        t.addView(tv, ll);
        t.setOnClickListener(v -> {
            if (launch == null) openDrawer(index);
            else launchApp(launch);
        });
        t.setOnLongClickListener(v -> {
            openDrawer(index);
            return true;
        });
        return t;
    }

    private String labelFor(String pkg) {
        String known = LABELS.get(pkg);
        if (known != null) return known;
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private void launchApp(Intent i) {
        try {
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ drawer

    private static class AppItem {
        String pkg, label;
        Drawable icon, modernIcon;
        Intent intent;
    }

    private void buildDrawer() {
        drawer.removeAllViews();
        drawer.setBackgroundColor(light ? 0xFAEAF4FC : 0xFA060A14);
        drawer.setClickable(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(2.6f), px(2.4f), px(2.6f), px(2f));
        drawer.addView(col, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        drawerTitle = text(2.2f, fg(), 600);
        drawerTitle.setText("Semua aplikasi");
        head.addView(drawerTitle, lp(0, -2, 1f));
        TextView update = text(1.6f, light ? Color.WHITE : 0xFF04121C, 700);
        update.setText("Kemaskini apps");
        update.setPadding(px(2f), px(.8f), px(2f), px(.8f));
        update.setBackground(glass(true, 2f));
        update.setOnClickListener(v -> openUpdates());
        LinearLayout.LayoutParams ul = lp(-2, -2, 0);
        ul.rightMargin = px(1.2f);
        head.addView(update, ul);
        TextView close = text(1.6f, fg(), 500);
        close.setText("Tutup");
        close.setPadding(px(2f), px(.8f), px(2f), px(.8f));
        close.setBackground(glass(false, 2f));
        close.setOnClickListener(v -> closeDrawer());
        LinearLayout.LayoutParams cll = lp(-2, -2, 0);
        cll.rightMargin = px(5.2f); // clear of the unit's floating button
        head.addView(close, cll);
        col.addView(head);

        GridLayout g = new GridLayout(this);
        g.setColumnCount(6);
        for (AppItem a : collectApps()) g.addView(drawerTile(a));
        ScrollView sv = new ScrollView(this);
        sv.setPadding(0, px(1.6f), 0, 0);
        sv.addView(g);
        col.addView(sv, lp(-1, 0, 1f));
    }

    private List<AppItem> collectApps() {
        PackageManager pm = getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> all = pm.queryIntentActivities(q, 0);
        List<AppItem> pinned = new ArrayList<>(), rest = new ArrayList<>();
        outer:
        for (ResolveInfo ri : all) {
            String pkg = ri.activityInfo.packageName;
            for (String h : HIDDEN) if (h.equals(pkg)) continue outer;
            AppItem a = new AppItem();
            a.pkg = pkg;
            a.label = LABELS.containsKey(pkg) ? LABELS.get(pkg) : String.valueOf(ri.loadLabel(pm));
            a.icon = ri.loadIcon(pm);
            a.intent = pm.getLaunchIntentForPackage(pkg);
            if (a.intent == null) continue;
            a.modernIcon = Icons.forApp(this, pkg, a.intent);
            int idx = -1;
            for (int i = 0; i < PINNED.length; i++) if (PINNED[i].equals(pkg)) idx = i;
            if (idx >= 0) {
                while (pinned.size() <= idx) pinned.add(null);
                pinned.set(idx, a);
            } else rest.add(a);
        }
        pinned.removeAll(Collections.singleton(null));
        Collections.sort(rest, (a, b) -> a.label.compareToIgnoreCase(b.label));
        pinned.addAll(rest);
        return pinned;
    }

    private View drawerTile(final AppItem a) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setBackground(glass(false, 1.6f));
        c.setClickable(true);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(a.modernIcon != null ? a.modernIcon : a.icon);
        c.addView(iv, new LinearLayout.LayoutParams(px(5f), px(5f)));
        TextView tv = text(1.4f, fg(), 500);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setText(a.label);
        tv.setPadding(px(.3f), px(.6f), px(.3f), 0);
        c.addView(tv);
        c.setOnClickListener(v -> {
            if (pick >= 0) {
                tiles[pick] = a.pkg;
                saveTiles();
                closeDrawer();
                rebuild();
            } else {
                closeDrawer();
                launchApp(a.intent);
            }
        });
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = px(14.2f);
        lp.height = px(11f);
        lp.setMargins(px(.4f), px(.4f), px(.4f), px(.4f));
        c.setLayoutParams(lp);
        return c;
    }

    /**
     * One tap to Play Store. Auto-update is switched off to keep the unit light, so updates are done by hand
     * from here. Play Store has no public link straight to "Manage apps", so a hint says where "Update all" is.
     */
    private void openUpdates() {
        closeDrawer();
        Intent open = getPackageManager().getLaunchIntentForPackage("com.android.vending");
        if (open == null) {
            Toast.makeText(this, "Play Store tiada", Toast.LENGTH_SHORT).show();
            return;
        }
        launchApp(open);
        Toast.makeText(this, "Ketik gambar akaun  ›  Urus apps & peranti  ›  Kemas kini semua",
                Toast.LENGTH_LONG).show();
    }

    private void openDrawer(int forIndex) {
        pick = forIndex;
        drawerTitle.setText(forIndex >= 0
                ? "Pilih aplikasi pengganti untuk \"" + (tiles[forIndex] == null ? "kotak kosong" : labelFor(tiles[forIndex])) + "\""
                : "Semua aplikasi");
        drawer.setVisibility(View.VISIBLE);
    }

    private void closeDrawer() {
        pick = -1;
        drawer.setVisibility(View.GONE);
    }

    private void loadTiles() {
        String saved = prefs.getString("tiles", null);
        String[] parts = saved == null ? new String[0] : saved.split(",", -1);
        for (int i = 0; i < 6; i++) {
            String v = i < parts.length ? parts[i] : DEFAULT_TILES[i];
            tiles[i] = v.isEmpty() ? DEFAULT_TILES[i] : v;
        }
    }

    private void saveTiles() {
        prefs.edit().putString("tiles", TextUtils.join(",", tiles)).apply();
    }

    // ------------------------------------------------------------------ data + refresh

    /** Prayer times, next prayer and how far into daytime we are (for the day/night fade). */
    private void computeTimes() {
        Calendar now = Calendar.getInstance();
        double tz = now.getTimeZone().getOffset(now.getTimeInMillis()) / 3600000.0;
        times = PrayerTimes.compute(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), lat, lng, tz);
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        nextPrayer = -1;
        for (int i = 0; i < 6; i++) {
            if (times[i] > nowMin) {
                nextPrayer = i;
                break;
            }
        }
        int minsLeft;
        if (nextPrayer >= 0) {
            minsLeft = times[nextPrayer] - nowMin;
            nextLabel = PrayerTimes.NAMES[nextPrayer] + " " + fmt(times[nextPrayer]);
        } else {
            Calendar tm = (Calendar) now.clone();
            tm.add(Calendar.DAY_OF_MONTH, 1);
            int[] t2 = PrayerTimes.compute(tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1,
                    tm.get(Calendar.DAY_OF_MONTH), lat, lng, tz);
            minsLeft = 24 * 60 - nowMin + t2[0];
            nextLabel = PrayerTimes.NAMES[0] + " " + fmt(t2[0]);
        }
        nextLabel = "Seterusnya: " + nextLabel + "  ·  "
                + (minsLeft >= 60 ? (minsLeft / 60) + " j " : "") + (minsLeft % 60) + " min lagi";

        float fade = 42f;
        float rise = times[1], set = times[4];
        float up = smooth((nowMin - (rise - fade / 2f)) / fade);
        float down = 1f - smooth((nowMin - (set - fade / 2f)) / fade);
        dayFactor = dayOverride >= 0f ? dayOverride : Math.min(up, down);
    }

    private static float smooth(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }

    /** Recompute time-driven state and redraw; swaps theme when day/night or rain flips the style. */
    private void update() {
        computeTimes();
        bg.setDayFactor(dayFactor);
        rainView.setVisibility(raining ? View.VISIBLE : View.GONE);
        bg.setRain(raining);
        boolean wantLight = dayFactor >= .5f && !raining;
        if (wantLight != light) {
            light = wantLight;
            rebuild();
        } else {
            refreshTexts();
        }
    }

    private void refreshTexts() {
        Date now = new Date();
        boolean h12 = !DateFormat.is24HourFormat(this);
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY), m = cal.get(Calendar.MINUTE);
        clockTv.setText(h12 ? String.format(Locale.US, "%d:%02d", h % 12 == 0 ? 12 : h % 12, m)
                : String.format(Locale.US, "%02d:%02d", h, m));
        ampmTv.setVisibility(h12 ? View.VISIBLE : View.GONE);
        ampmTv.setText(h < 12 ? "AM" : "PM");
        dateTv.setText(new SimpleDateFormat("EEEE, d MMMM", new Locale("ms", "MY")).format(now));
        for (int i = 0; i < 6; i++) {
            prayerTime[i].setText(fmt(times[i]));
            boolean on = i == nextPrayer || (nextPrayer < 0 && i == 0);
            prayerCell[i].setBackground(glassBg(prayerCell[i], 1.1f, on, false));
            prayerName[i].setTextColor(on ? accent() : fgSoft());
            prayerTime[i].setTextColor(on ? accent() : fg());
        }
        nextLineTv.setText(nextLabel);
        tripTv.setText(String.format(Locale.US, "Perjalanan %.1f km", tripMeters / 1000f));
        refreshWeather();
        updateChips();
    }

    private void refreshWeather() {
        boolean have = wxTemp != null;
        long age = System.currentTimeMillis() - wxAt;
        boolean stale = have && !internet;
        locTv.setText("📍 " + (!hasFix ? "Menunggu GPS" : wxPlace != null ? wxPlace : "Lokasi")
                + (stale ? "  (disimpan)" : ""));
        float dim = stale ? .6f : 1f;
        wxIconTv.setAlpha(dim);
        wxTempTv.setAlpha(dim);
        wxCondTv.setAlpha(dim);
        if (have) {
            wxIconTv.setText(WeatherClient.icon(wxCode));
            wxTempTv.setText(wxTemp + "°C");
            wxCondTv.setText(stale ? "Kemas kini " + agoText(age) : WeatherClient.describe(wxCode));
        } else {
            wxIconTv.setText("⛅");
            wxTempTv.setText("--°C");
            wxCondTv.setText("Tiada data cuaca");
        }
        wxHintTv.setText(!hasFix ? "Cuaca muncul selepas GPS dapat isyarat"
                : internet ? "" : "Sambung Wi-Fi atau hotspot untuk cuaca");
    }

    private static String agoText(long ms) {
        long min = ms / 60000;
        if (min < 60) return Math.max(1, min) + " min lalu";
        long h = min / 60;
        return h < 48 ? h + " j lalu" : (h / 24) + " hari lalu";
    }

    private void updateChips() {
        if (chipTv[0] == null) return;
        setChip(0, wifiOn ? (ssid != null ? ssid : "Wi-Fi") : "Tiada Wi-Fi", wifiOn ? okColor() : badColor());
        setChip(1, btOn ? (btName != null ? btName : "Bluetooth") : "Bluetooth mati",
                btName != null ? okColor() : fgSoft());
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsOn) setChip(2, "GPS mati", badColor());
        else if (satUsed > 0) setChip(2, "GPS · " + satUsed + " sat", okColor());
        else setChip(2, "GPS", fgSoft());
    }

    private void setChip(int i, String label, int color) {
        chipTv[i].setText(label);
        chipIv[i].setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void pollNet() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
            internet = nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm.isWifiEnabled() ? wm.getConnectionInfo() : null;
            wifiOn = wi != null && wi.getNetworkId() != -1;
            ssid = null;
            if (wifiOn) {
                String s = wi.getSSID();
                if (s != null && !s.contains("unknown ssid") && !s.equals("0x")) ssid = s.replace("\"", "");
            }
        } catch (Exception ignored) {
        }
    }

    private void pollBluetooth() {
        try {
            final BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
            btOn = ad != null && ad.isEnabled();
            if (!btOn) {
                btName = null;
                return;
            }
            final String[] found = {null};
            final int[] pending = {2};
            BluetoothProfile.ServiceListener l = new BluetoothProfile.ServiceListener() {
                @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    try {
                        List<BluetoothDevice> d = proxy.getConnectedDevices();
                        if (!d.isEmpty() && found[0] == null) found[0] = d.get(0).getName();
                    } catch (Exception ignored) {
                    }
                    ad.closeProfileProxy(profile, proxy);
                    if (--pending[0] == 0) {
                        btName = found[0];
                        updateChips();
                    }
                }

                @Override public void onServiceDisconnected(int profile) {
                }
            };
            if (!ad.getProfileProxy(this, l, BluetoothProfile.A2DP)) pending[0]--;
            if (!ad.getProfileProxy(this, l, BluetoothProfile.HEADSET)) pending[0]--;
            if (pending[0] == 0) btName = null;
        } catch (Exception ignored) {
        }
    }

    private void maybeFetchWeather() {
        if (!internet || fetching || !hasFix) return;
        long now = System.currentTimeMillis();
        if (wxTemp != null && now - wxAt < WEATHER_EVERY_MS) return;
        fetching = true;
        final double la = lat, lo = lng;
        new Thread(() -> {
            try {
                final WeatherClient.Result r = WeatherClient.fetch(la, lo);
                handler.post(() -> {
                    wxTemp = r.temp;
                    wxCode = r.code;
                    if (r.place != null) wxPlace = r.place;
                    wxAt = System.currentTimeMillis();
                    prefs.edit().putInt("wxTemp", r.temp).putInt("wxCode", r.code)
                            .putString("wxPlace", wxPlace).putLong("wxAt", wxAt)
                            .putBoolean("wxFix", true).apply();
                    boolean rain = WeatherClient.isRain(r.code);
                    if (rain != raining) {
                        raining = rain;
                        update();
                    } else {
                        refreshTexts();
                    }
                });
            } catch (Exception ignored) {
            } finally {
                fetching = false;
            }
        }).start();
    }

    private static String fmt(int minutes) {
        return String.format(Locale.US, "%02d:%02d", (minutes / 60) % 24, minutes % 60);
    }

    // ------------------------------------------------------------------ lifecycle + GPS

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        startGps();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) startGps();
    }

    private void startGps() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) useLocation(last);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
            if (!gnssRegistered) {
                gnssRegistered = lm.registerGnssStatusCallback(gnss, handler);
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
    }

    private void useLocation(Location l) {
        hasFix = true;
        lat = l.getLatitude();
        lng = l.getLongitude();
        prefs.edit().putLong("lat", Double.doubleToLongBits(lat))
                .putLong("lng", Double.doubleToLongBits(lng)).apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lm.removeUpdates(this);
        if (gnssRegistered) {
            lm.unregisterGnssStatusCallback(gnss);
            gnssRegistered = false;
        }
    }

    @Override
    public void onLocationChanged(Location l) {
        gpsLog.record(l, satUsed);
        lastSpeed = l.hasSpeed() ? l.getSpeed() * 3.6f : 0f;
        speedo.setSpeed(lastSpeed);
        if (lastFix != null && lastSpeed > 3f) tripMeters += lastFix.distanceTo(l);
        lastFix = l;
        if (Math.abs(l.getLatitude() - lat) > .05 || Math.abs(l.getLongitude() - lng) > .05) {
            useLocation(l);
            wxAt = 0;
            update();
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        closeDrawer();
        if (i == null) return;
        // Test hooks (adb): --ez splash true, --ez rain true|false, --ef day 0|1 (-1 clears).
        if (i.getBooleanExtra("splash", false)) splash.play(null);
        if (i.hasExtra("rain")) raining = i.getBooleanExtra("rain", false);
        if (i.hasExtra("day")) dayOverride = i.getFloatExtra("day", -1f);
        if (i.getBooleanExtra("gpslogtest", false)) {
            Location fake = new Location("gps");
            fake.setSpeed(16.7f);
            fake.setAccuracy(5f);
            fake.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos() - 80_000_000L);
            gpsLog.record(fake, 7);
        }
        if (i.hasExtra("speed")) {
            lastSpeed = i.getFloatExtra("speed", 0f);
            speedo.setSpeedImmediate(lastSpeed);
        }
        if (i.hasExtra("rain") || i.hasExtra("day")) update();
    }

    @Override
    public void onBackPressed() {
        if (drawer.getVisibility() == View.VISIBLE) closeDrawer();
    }
}
