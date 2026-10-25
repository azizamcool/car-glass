package my.carglass;

import android.content.Context;
import android.graphics.Typeface;
import android.util.SparseArray;

/** Exo 2 (variable font, SIL OFL) from assets, falling back to the system font. */
final class Fonts {
    private static final SparseArray<Typeface> CACHE = new SparseArray<>();

    private Fonts() {
    }

    static Typeface get(Context c, int weight) {
        Typeface t = CACHE.get(weight);
        if (t != null) return t;
        try {
            t = new Typeface.Builder(c.getAssets(), "fonts/Exo2.ttf")
                    .setFontVariationSettings("'wght' " + weight).build();
        } catch (Exception ignored) {
        }
        if (t == null) t = Typeface.create(Typeface.DEFAULT, weight, false);
        CACHE.put(weight, t);
        return t;
    }
}
