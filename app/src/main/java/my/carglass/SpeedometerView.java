package my.carglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Sporty segmented gauge: 50 lit segments, red zone, needle marker, italic digits. */
public class SpeedometerView extends View {
    private static final float MAX = 200f, START = 135f, SWEEP = 270f;
    private static final int N = 50;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path needle = new Path();
    private float target, shown;
    private boolean light;
    private int accent = 0xFF4FC3F7;

    public SpeedometerView(Context c) {
        super(c);
    }

    public void setTheme(boolean light, int accent) {
        this.light = light;
        this.accent = accent;
        invalidate();
    }

    public void setSpeed(float kmh) {
        target = Math.max(0f, Math.min(MAX, kmh));
        postInvalidateOnAnimation();
    }

    public void setSpeedImmediate(float kmh) {
        target = shown = Math.max(0f, Math.min(MAX, kmh));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        shown += (target - shown) * 0.18f;
        boolean settling = Math.abs(target - shown) > 0.25f;
        if (!settling) shown = target;

        float s = Math.min(getWidth(), getHeight());
        float k = s / 400f, cx = getWidth() / 2f, cy = getHeight() / 2f;
        int fg = light ? 0xFF0D1B2E : 0xFFFFFFFF;

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f * k);
        p.setColor((accent & 0x00FFFFFF) | 0x4D000000);
        c.drawCircle(cx, cy, 192 * k, p);
        p.setColor((accent & 0x00FFFFFF) | 0x59000000);
        c.drawCircle(cx, cy, 122 * k, p);

        float r = 160 * k;
        oval.set(cx - r, cy - r, cx + r, cy + r);
        p.setStrokeWidth(18 * k);
        p.setStrokeCap(Paint.Cap.BUTT);
        float lit = shown / MAX * N;
        for (int i = 0; i < N; i++) {
            float fr = i / (float) N;
            boolean on = i + .5f <= lit;
            int col;
            if (on) col = fr < .6f ? accent : fr < .8f ? 0xFFFFB74D : 0xFFFF5252;
            else if (fr >= .8f) col = light ? 0x4DDC3232 : 0x4DFF5252;
            else col = light ? 0x290D1B2E : 0x24FFFFFF;
            p.setColor(col);
            c.drawArc(oval, START + SWEEP * i / N + .7f, SWEEP / N - 1.4f, false, p);
        }

        // Needle marker just inside the segments, rotated to the current speed.
        float ang = START + SWEEP * shown / MAX + 90f;
        c.save();
        c.rotate(ang, cx, cy);
        needle.rewind();
        needle.moveTo(cx, cy - 148 * k);
        needle.lineTo(cx - 10 * k, cy - 122 * k);
        needle.lineTo(cx + 10 * k, cy - 122 * k);
        needle.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(light ? 0xFF0D1B2E : 0xFFFFFFFF);
        c.drawPath(needle, p);
        c.restore();

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSkewX(-.2f);
        p.setTypeface(Fonts.get(getContext(), 500));
        p.setTextSize(12 * k);
        p.setColor(light ? 0xB30D1B2E : 0x99FFFFFF);
        for (int v = 0; v <= MAX; v += 40) {
            double a = Math.toRadians(START + SWEEP * v / MAX);
            c.drawText(String.valueOf(v), cx + (float) (100 * k * Math.cos(a)),
                    cy + (float) (100 * k * Math.sin(a)) + 4 * k, p);
        }
        p.setTextSkewX(0f);

        p.setTypeface(Fonts.get(getContext(), 700));
        p.setLetterSpacing(.5f);
        p.setTextSize(13 * k);
        p.setColor(accent);
        c.drawText("SPORT", cx, cy - 54 * k, p);

        p.setLetterSpacing(-.03f);
        p.setTypeface(Fonts.get(getContext(), 800));
        p.setTextSkewX(-.25f);
        p.setTextSize((shown >= 99.5f ? 96 : 118) * k);
        p.setColor(fg);
        c.drawText(String.valueOf(Math.round(shown)), cx, cy + 36 * k, p);
        p.setTextSkewX(0f);

        p.setLetterSpacing(.33f);
        p.setTypeface(Fonts.get(getContext(), 600));
        p.setTextSize(18 * k);
        p.setColor(light ? 0xA60D1B2E : 0xA6FFFFFF);
        c.drawText("KM/J", cx, cy + 68 * k, p);
        p.setLetterSpacing(0f);

        if (settling) postInvalidateOnAnimation();
    }
}
