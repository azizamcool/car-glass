package my.carglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The "now playing" card under the speedometer: playing, or a few shortcuts when nothing is. */
final class MediaCard extends LinearLayout {
    interface Actions {
        void previous();

        void toggle();

        void next();

        /** which: "ytm", "radio" or "bt" */
        void open(String which);

        void grantAccess();
    }

    private final float cq;
    private final boolean light;
    private final int accent, fg, fgSoft;
    private final Actions actions;

    private final ImageView art;
    private final TextView src, title, sub;
    private final ProgressLine bar;
    private final LinearLayout controls, pills;
    private final PlayerButton playBtn;

    MediaCard(Context c, float cq, boolean light, int accent, int fg, int fgSoft, Actions actions) {
        super(c);
        this.cq = cq;
        this.light = light;
        this.accent = accent;
        this.fg = fg;
        this.fgSoft = fgSoft;
        this.actions = actions;
        setOrientation(VERTICAL);
        setPadding(px(1.4f), px(.9f), px(1.4f), px(.9f));

        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        art = new ImageView(c);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        final float r = px(1.2f);
        art.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        art.setClipToOutline(true);
        top.addView(art, new LayoutParams(px(5.2f), px(5.2f)));

        LinearLayout meta = new LinearLayout(c);
        meta.setOrientation(VERTICAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        src = text(1.15f, accent, 700);
        src.setLetterSpacing(.14f);
        title = text(1.9f, fg, 700);
        sub = text(1.3f, fgSoft, 500);
        bar = new ProgressLine(c);
        meta.addView(src);
        meta.addView(title);
        meta.addView(sub);
        LayoutParams bl = new LayoutParams(-1, Math.max(2, px(.45f)));
        bl.topMargin = px(.6f);
        meta.addView(bar, bl);
        LayoutParams ml = new LayoutParams(0, -2, 1f);
        ml.leftMargin = px(1.2f);
        top.addView(meta, ml);
        addView(top);

        controls = new LinearLayout(c);
        controls.setGravity(Gravity.CENTER);
        PlayerButton prev = new PlayerButton(c, PlayerButton.PREV, false);
        playBtn = new PlayerButton(c, PlayerButton.PAUSE, true);
        PlayerButton next = new PlayerButton(c, PlayerButton.NEXT, false);
        prev.setOnClickListener(v -> actions.previous());
        playBtn.setOnClickListener(v -> actions.toggle());
        next.setOnClickListener(v -> actions.next());
        controls.addView(prev, new LayoutParams(px(4f), px(4f)));
        LayoutParams pl = new LayoutParams(px(5f), px(5f));
        pl.setMargins(px(2.2f), 0, px(2.2f), 0);
        controls.addView(playBtn, pl);
        controls.addView(next, new LayoutParams(px(4f), px(4f)));
        LayoutParams cl = new LayoutParams(-1, -2);
        cl.topMargin = px(.8f);
        addView(controls, cl);

        pills = new LinearLayout(c);
        pills.addView(pill("YouTube Music", true, "ytm"), new LayoutParams(0, -2, 1.6f));
        pills.addView(pill("Radio", false, "radio"), pillParams());
        pills.addView(pill("BT", false, "bt"), pillParams());
        LayoutParams pil = new LayoutParams(-1, -2);
        pil.topMargin = px(.8f);
        addView(pills, pil);
    }

    private LayoutParams pillParams() {
        LayoutParams p = new LayoutParams(0, -2, 1f);
        p.leftMargin = px(.7f);
        return p;
    }

    private TextView pill(String label, boolean main, String which) {
        TextView t = text(1.3f, main ? (light ? 0xFFFFFFFF : 0xFF04121C) : fg, 600);
        t.setGravity(Gravity.CENTER);
        t.setText(label);
        t.setPadding(px(.6f), px(.8f), px(.6f), px(.8f));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(px(3f));
        g.setColor(main ? accent : light ? 0xB3FFFFFF : 0x1AFFFFFF);
        g.setStroke(Math.max(1, px(.1f)), main ? accent : light ? 0xF2FFFFFF : 0x38FFFFFF);
        t.setBackground(g);
        t.setOnClickListener(v -> actions.open(which));
        return t;
    }

    private int px(float u) {
        return Math.round(u * cq);
    }

    private TextView text(float size, int color, int weight) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * cq);
        t.setTextColor(color);
        t.setTypeface(Fonts.get(getContext(), weight));
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    /** s == null: nothing is playing. */
    void show(MediaWatcher.State s, boolean hasAccess) {
        if (s == null) {
            src.setText("MEDIA");
            src.setTextColor(fgSoft);
            title.setText("Tiada media");
            sub.setText(hasAccess ? "Ketik untuk mula" : "Ketik sini: beri akses media");
            sub.setOnClickListener(hasAccess ? null : v -> actions.grantAccess());
            sub.setClickable(!hasAccess);
            art.setImageResource(R.drawable.ph_music_notes);
            art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            art.setPadding(px(1f), px(1f), px(1f), px(1f));
            art.setColorFilter(accent);
            art.setBackground(rounded(light ? 0x99FFFFFF : 0x1FFFFFFF));
            bar.setVisibility(GONE);
            controls.setVisibility(GONE);
            pills.setVisibility(VISIBLE);
            return;
        }
        src.setText(s.source.toUpperCase());
        src.setTextColor(accent);
        title.setText(s.title);
        sub.setText(s.artist);
        sub.setOnClickListener(null);
        sub.setClickable(false);
        art.setPadding(0, 0, 0, 0);
        art.setColorFilter(null);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (s.art != null) {
            art.setBackground(null);
            art.setImageBitmap(s.art);
        } else {
            art.setImageResource(R.drawable.ph_music_notes);
            art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            art.setPadding(px(1f), px(1f), px(1f), px(1f));
            art.setColorFilter(0xFFFFFFFF);
            GradientDrawable g = rounded(accent);
            art.setBackground(g);
        }
        bar.setVisibility(s.duration > 0 ? VISIBLE : INVISIBLE);
        bar.set(s.duration > 0 ? (float) s.position / s.duration : 0f, accent, light ? 0x2E0D1B2E : 0x33FFFFFF);
        playBtn.setKind(s.playing ? PlayerButton.PAUSE : PlayerButton.PLAY);
        controls.setVisibility(VISIBLE);
        pills.setVisibility(GONE);
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(1.2f));
        return g;
    }

    /** A thin progress line. */
    private static final class ProgressLine extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float frac;
        private int on, off;

        ProgressLine(Context c) {
            super(c);
        }

        void set(float frac, int on, int off) {
            this.frac = frac;
            this.on = on;
            this.off = off;
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            float h = getHeight(), w = getWidth();
            p.setColor(off);
            c.drawRoundRect(new RectF(0, 0, w, h), h / 2, h / 2, p);
            p.setColor(on);
            c.drawRoundRect(new RectF(0, 0, Math.max(h, w * frac), h), h / 2, h / 2, p);
        }
    }

    /** Round transport button drawn with paths (no icon font needed). */
    private final class PlayerButton extends View {
        static final int PREV = 0, PLAY = 1, PAUSE = 2, NEXT = 3;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final boolean main;
        private int kind;

        PlayerButton(Context c, int kind, boolean main) {
            super(c);
            this.kind = kind;
            this.main = main;
            setClickable(true);
        }

        void setKind(int k) {
            if (kind != k) {
                kind = k;
                invalidate();
            }
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, r = Math.min(w, h) / 2 - 1;
            p.setStyle(Paint.Style.FILL);
            p.setColor(main ? accent : light ? 0xB3FFFFFF : 0x1AFFFFFF);
            c.drawCircle(cx, cy, r, p);
            if (!main) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, cq * .1f));
                p.setColor(light ? 0xF2FFFFFF : 0x38FFFFFF);
                c.drawCircle(cx, cy, r, p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(main ? (light ? 0xFFFFFFFF : 0xFF04121C) : fg);
            float s = r * .42f;
            path.reset();
            switch (kind) {
                case PLAY:
                    path.moveTo(cx - s * .6f, cy - s);
                    path.lineTo(cx + s, cy);
                    path.lineTo(cx - s * .6f, cy + s);
                    path.close();
                    c.drawPath(path, p);
                    break;
                case PAUSE:
                    c.drawRoundRect(new RectF(cx - s * .85f, cy - s, cx - s * .2f, cy + s), 2, 2, p);
                    c.drawRoundRect(new RectF(cx + s * .2f, cy - s, cx + s * .85f, cy + s), 2, 2, p);
                    break;
                case PREV:
                    c.drawRect(cx - s, cy - s * .85f, cx - s * .55f, cy + s * .85f, p);
                    path.moveTo(cx + s, cy - s * .85f);
                    path.lineTo(cx - s * .4f, cy);
                    path.lineTo(cx + s, cy + s * .85f);
                    path.close();
                    c.drawPath(path, p);
                    break;
                default: // NEXT
                    c.drawRect(cx + s * .55f, cy - s * .85f, cx + s, cy + s * .85f, p);
                    path.moveTo(cx - s, cy - s * .85f);
                    path.lineTo(cx + s * .4f, cy);
                    path.lineTo(cx - s, cy + s * .85f);
                    path.close();
                    c.drawPath(path, p);
            }
        }
    }
}
