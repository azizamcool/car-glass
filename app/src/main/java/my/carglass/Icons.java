package my.carglass;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;

import java.util.HashMap;
import java.util.Map;

/**
 * Icons without the square plate. Known head-unit apps get a duotone glyph (Phosphor, MIT); apps whose adaptive icon
 * sits on a plain light background show only the glyph layer; everything else keeps its own icon.
 */
final class Icons {
    private static final class Glyph {
        final int res, color;

        Glyph(int res, int color) {
            this.res = res;
            this.color = color;
        }
    }

    private static final Map<String, Glyph> GLYPHS = new HashMap<>();

    static {
        GLYPHS.put("com.imotor.fmam", new Glyph(R.drawable.ph_radio, 0xFFFF6B6B));
        GLYPHS.put("com.imotor.btmusic", new Glyph(R.drawable.ph_bluetooth, 0xFF5B9BFF));
        GLYPHS.put("com.imotor.music", new Glyph(R.drawable.ph_music_notes, 0xFFC084FC));
        GLYPHS.put("com.imotor.video", new Glyph(R.drawable.ph_film_strip, 0xFFFB923C));
        GLYPHS.put("com.imotor.navigation", new Glyph(R.drawable.ph_navigation_arrow, 0xFF34D399));
        GLYPHS.put("com.imotor.phoneconnect", new Glyph(R.drawable.ph_car_profile, 0xFF2DD4BF));
        GLYPHS.put("com.imotor.filemanager", new Glyph(R.drawable.ph_folder_simple, 0xFFFBBF24));
        GLYPHS.put("com.android.gallery3d", new Glyph(R.drawable.ph_image, 0xFF60A5FA));
        GLYPHS.put("com.android.settings", new Glyph(R.drawable.ph_gear_six, 0xFF94A3B8));
    }

    private Icons() {
    }

    static Drawable forApp(Context c, String pkg, Intent launch) {
        PackageManager pm = c.getPackageManager();
        Glyph g = GLYPHS.get(pkg);
        if (g != null) {
            Drawable d = c.getDrawable(g.res).mutate();
            d.setTint(g.color);
            return d;
        }
        Drawable icon;
        try {
            icon = launch != null ? pm.getActivityIcon(launch) : pm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable ad = (AdaptiveIconDrawable) icon;
            Drawable bg = ad.getBackground(), fg = ad.getForeground();
            if (bg != null && fg != null && isPlainLight(bg)) {
                // The foreground layer lives on a 108dp canvas with the glyph inside the central 72dp: enlarge it.
                Drawable big = new InsetDrawable(fg, -0.25f);
                // Some apps paint their own white plate into the foreground as well.
                Drawable cut = stripLightPlate(c, big);
                return cut != null ? cut : big;
            }
            return icon;
        }
        Drawable cut = stripLightPlate(c, icon);
        return cut != null ? cut : icon;
    }

    /**
     * Old-style (non-adaptive) icons often come on an opaque white plate. Make the white that touches the edge
     * transparent so only the glyph remains. Returns null when the icon has no such plate.
     */
    private static Drawable stripLightPlate(Context c, Drawable icon) {
        try {
            int n = 96;
            Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            icon.setBounds(0, 0, n, n);
            icon.draw(cv);
            int[] px = new int[n * n];
            b.getPixels(px, 0, n, 0, 0, n, n);
            if (!isPlate(px[0]) || !isPlate(px[n - 1]) || !isPlate(px[n * (n - 1)]) || !isPlate(px[n * n - 1])) return null;
            java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
            boolean[] seen = new boolean[n * n];
            for (int i = 0; i < n; i++) {
                q.add(i);
                q.add(n * (n - 1) + i);
                q.add(i * n);
                q.add(i * n + n - 1);
            }
            int cleared = 0;
            while (!q.isEmpty()) {
                int p = q.poll();
                if (seen[p] || !isPlate(px[p])) continue;
                seen[p] = true;
                px[p] = 0;
                cleared++;
                int x = p % n, y = p / n;
                if (x > 0) q.add(p - 1);
                if (x < n - 1) q.add(p + 1);
                if (y > 0) q.add(p - n);
                if (y < n - 1) q.add(p + n);
            }
            if (cleared < n * n / 8) return null;
            b.setPixels(px, 0, n, 0, 0, n, n);
            return new android.graphics.drawable.BitmapDrawable(c.getResources(), b);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isPlate(int p) {
        return Color.alpha(p) > 240 && Color.red(p) > 238 && Color.green(p) > 238 && Color.blue(p) > 238;
    }

    /** True when the layer is one flat, light colour (typically white). */
    private static boolean isPlainLight(Drawable d) {
        try {
            Bitmap b = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            d.setBounds(0, 0, 8, 8);
            d.draw(cv);
            int first = b.getPixel(0, 0);
            float lum = 0f;
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    int p = b.getPixel(x, y);
                    if (Color.alpha(p) < 250) return false;
                    if (Math.abs(Color.red(p) - Color.red(first)) > 12 || Math.abs(Color.green(p) - Color.green(first)) > 12
                            || Math.abs(Color.blue(p) - Color.blue(first)) > 12) return false;
                    lum += (Color.red(p) * .299f + Color.green(p) * .587f + Color.blue(p) * .114f) / 255f;
                }
            }
            b.recycle();
            return lum / 64f > .88f;
        } catch (Exception e) {
            return false;
        }
    }
}
