package my.carglass;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** NEXUP logo: a big play triangle sweeps in revealing the wordmark, then settles (about 1.3 s). */
public class SplashView extends View {
    private static final int TOTAL_MS = 1300;
    private static final float SWEEP_FRACTION = 850f / TOTAL_MS;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final DecelerateInterpolator ease = new DecelerateInterpolator(1.6f);
    private ValueAnimator anim;
    private float t;
    private Runnable onEnd;

    public SplashView(Context c) {
        super(c);
        setClickable(true);
        setVisibility(GONE);
    }

    public void play(Runnable end) {
        onEnd = end;
        if (anim != null) anim.cancel();
        setVisibility(VISIBLE);
        setAlpha(1f);
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(TOTAL_MS);
        anim.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                setVisibility(GONE);
                if (onEnd != null) onEnd.run();
            }
        });
        anim.start();
    }

    private static float lerp(float a, float b, float f) {
        return a + (b - a) * f;
    }

    /** Keyframes of nexBody: how much of the wordmark is revealed. */
    private static float reveal(float q) {
        if (q <= .10f) return lerp(0f, .03f, q / .10f);
        if (q <= .58f) return lerp(.03f, .96f, (q - .10f) / .48f);
        if (q <= .75f) return lerp(.96f, 1f, (q - .58f) / .17f);
        return 1f;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        p.setShader(new RadialGradient(w / 2, h * .42f, Math.max(w, h) * .7f,
                0xFF12203A, 0xFF05070F, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        float fade = t < .1f ? t / .1f : t > .82f ? Math.max(0f, (1f - t) / .18f) : 1f;
        int alpha = Math.round(fade * 255);

        float q = ease.getInterpolation(Math.min(1f, t / SWEEP_FRACTION));
        float em = w * .095f;
        p.setTypeface(Fonts.get(getContext(), 800));
        p.setTextSize(em);
        p.setLetterSpacing(-.015f);
        float nexW = p.measureText("NEX"), upW = p.measureText("UP");
        float chip = em * 1.45f, gap = em * .36f;
        float total = chip + gap + nexW + upW;
        float left = (w - total) / 2f, cy = h / 2f;

        c.save();
        c.clipRect(left - em * .1f, 0, left + (total + em * .1f) * reveal(q), h);
        drawChip(c, left, cy - chip / 2f, chip, alpha);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.LEFT);
        float base = cy + em * .36f;
        p.setColor((0xFFFFFF) | (alpha << 24));
        c.drawText("NEX", left + chip + gap, base, p);
        p.setColor((0xE63329) | (alpha << 24));
        c.drawText("UP", left + chip + gap + nexW, base, p);
        c.restore();

        // Play triangle: starts large at the far left, sweeps right, shrinks into place.
        float triW = em * .46f, triH = em * .5f;
        float restX = left + total + em * .06f;
        float tp = Math.min(1f, q);
        float x, scale;
        if (tp <= .55f) {
            x = lerp(left - total * .1f, restX, tp / .55f);
            scale = 3.6f;
        } else {
            x = restX;
            scale = tp <= .75f ? lerp(3.6f, 1f, (tp - .55f) / .20f) : 1f;
        }
        c.save();
        c.translate(x, cy + em * .02f);
        c.scale(scale, scale);
        path.rewind();
        path.moveTo(-triW / 2, -triH / 2);
        path.lineTo(triW / 2, 0);
        path.lineTo(-triW / 2, triH / 2);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor((0xE63329) | (alpha << 24));
        c.drawPath(path, p);
        c.restore();
        p.setLetterSpacing(0f);
    }

    private void drawChip(Canvas c, float x, float y, float s, int alpha) {
        float k = s / 40f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xE63329 | (alpha << 24));
        rf.set(x + 4 * k, y + 4 * k, x + 36 * k, y + 36 * k);
        c.drawRoundRect(rf, 5 * k, 5 * k, p);
        for (int i = 0; i < 3; i++) {
            float o = 10 + i * 8;
            c.drawRect(x, y + o * k, x + 5 * k, y + (o + 4) * k, p);
            c.drawRect(x + 35 * k, y + o * k, x + 40 * k, y + (o + 4) * k, p);
            float q = 11 + i * 8;
            c.drawRect(x + q * k, y, x + (q + 4) * k, y + 5 * k, p);
            c.drawRect(x + q * k, y + 35 * k, x + (q + 4) * k, y + 40 * k, p);
        }
        p.setColor(0xC0392B | (alpha << 24));
        rf.set(x + 9 * k, y + 9 * k, x + 31 * k, y + 31 * k);
        c.drawRoundRect(rf, 3 * k, 3 * k, p);
        path.rewind();
        path.moveTo(x + 14 * k, y + 13 * k);
        path.lineTo(x + 14 * k, y + 27 * k);
        path.lineTo(x + 28 * k, y + 20 * k);
        path.close();
        p.setColor(0xFFFFFF | (Math.round(alpha * .95f) << 24));
        c.drawPath(path, p);
    }
}
