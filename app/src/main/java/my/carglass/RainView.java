package my.carglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/** Falling streaks plus a few droplets sliding down the "glass". Runs at ~30 fps while shown. */
public class RainView extends View {
    private static final int DROPS = 90, GLASS = 14;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private float[] x, y, len, vel, alpha;
    private float[] gx, gy, gr, gs, gw;
    private boolean ready;

    public RainView(Context c) {
        super(c);
        p.setStrokeCap(Paint.Cap.ROUND);
    }

    private void init() {
        float w = getWidth(), h = getHeight(), k = w / 1280f;
        x = new float[DROPS]; y = new float[DROPS]; len = new float[DROPS];
        vel = new float[DROPS]; alpha = new float[DROPS];
        for (int i = 0; i < DROPS; i++) {
            x[i] = rnd.nextFloat() * w * 1.2f;
            y[i] = rnd.nextFloat() * h;
            len[i] = (12 + rnd.nextFloat() * 22) * k;
            vel[i] = (16 + rnd.nextFloat() * 10) * k;
            alpha[i] = .18f + rnd.nextFloat() * .3f;
        }
        gx = new float[GLASS]; gy = new float[GLASS]; gr = new float[GLASS];
        gs = new float[GLASS]; gw = new float[GLASS];
        for (int i = 0; i < GLASS; i++) {
            gx[i] = rnd.nextFloat() * w;
            gy[i] = rnd.nextFloat() * h;
            gr[i] = (2 + rnd.nextFloat() * 4.5f) * k;
            gw[i] = rnd.nextFloat() * 200;
        }
        ready = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        ready = false;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!ready) init();
        float w = getWidth(), h = getHeight(), k = w / 1280f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.3f * k);
        for (int i = 0; i < DROPS; i++) {
            p.setColor(Math.round(alpha[i] * 255) << 24 | 0xC8E1FF);
            c.drawLine(x[i], y[i], x[i] - len[i] * .25f, y[i] + len[i], p);
            y[i] += vel[i];
            x[i] -= vel[i] * .22f;
            if (y[i] > h || x[i] < 0) {
                y[i] = -len[i];
                x[i] = rnd.nextFloat() * w * 1.2f;
            }
        }
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < GLASS; i++) {
            p.setShader(new RadialGradient(gx[i] - gr[i] * .3f, gy[i] - gr[i] * .3f, gr[i],
                    0xBFFFFFFF, 0x1FA0C8FF, Shader.TileMode.CLAMP));
            c.drawOval(gx[i] - gr[i], gy[i] - gr[i] * 1.25f, gx[i] + gr[i], gy[i] + gr[i] * 1.25f, p);
            p.setShader(null);
            if (gw[i] > 0) {
                gw[i]--;
            } else {
                if (gs[i] == 0) gs[i] = (rnd.nextFloat() * .9f + .2f) * k * 2;
                gy[i] += gs[i];
                gx[i] += Math.sin(gy[i] * .05f) * .2f;
                if (rnd.nextFloat() < .01f) {
                    gs[i] = 0;
                    gw[i] = 60 + rnd.nextFloat() * 180;
                }
                if (gy[i] > h + 10) {
                    gy[i] = -10;
                    gx[i] = rnd.nextFloat() * w;
                    gs[i] = 0;
                    gw[i] = rnd.nextFloat() * 120;
                }
            }
        }
        postInvalidateDelayed(33);
    }
}
