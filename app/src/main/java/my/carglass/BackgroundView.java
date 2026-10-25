package my.carglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/** Night road with a daytime scene faded in on top (dayFactor 0..1). */
public class BackgroundView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float day;
    private final int accent = 0xFF4FC3F7;

    public BackgroundView(Context c) {
        super(c);
    }

    private boolean rain;

    public void setRain(boolean r) {
        if (rain == r) return;
        rain = r;
        invalidate();
    }

    public void setDayFactor(float f) {
        f = Math.max(0f, Math.min(1f, f));
        if (Math.abs(f - day) < .004f) return;
        day = f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        drawNight(c, w, h);
        if (day > 0f) {
            int save = c.saveLayerAlpha(0, 0, w, h, Math.round(day * 255));
            drawDay(c, w, h);
            c.restoreToCount(save);
        }
        if (rain) {
            // Overcast: dim and cool the whole scene so light text stays readable.
            p.setShader(null);
            p.setColor(0xB3101A2B);
            c.drawRect(0, 0, w, h, p);
        }
    }

    private void drawNight(Canvas c, float w, float h) {
        float sx = w / 1280f, sy = h / 720f, horizon = 400 * sy;
        p.setColor(0xFF000000);
        p.setShader(new LinearGradient(0, 0, 0, h, new int[]{0xFF05070F, 0xFF1B2A4A, 0x8C4FC3F7},
                new float[]{0f, .6f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setColor(0xFF000000);
        p.setShader(new RadialGradient(w / 2, horizon, 230 * sx,
                new int[]{0xE64FC3F7, 0x004FC3F7}, null, Shader.TileMode.CLAMP));
        c.drawCircle(w / 2, horizon, 230 * sx, p);
        p.setShader(null);

        Random rnd = new Random(7);
        p.setColor(0xE60A1020);
        for (int i = 0; i < 14; i++) {
            float x = (i * 100 - 40) * sx, mh = (60 + rnd.nextFloat() * 110) * sy;
            tri(x, horizon, x + 60 * sx, horizon - mh, x + 130 * sx, horizon);
            c.drawPath(path, p);
        }
        p.setColor(0xFF070B14);
        c.drawRect(0, horizon, w, h, p);
        p.setColor(0xFF10192B);
        quad(610 * sx, horizon, 670 * sx, horizon, 1180 * sx, h, 100 * sx, h);
        c.drawPath(path, p);
        p.setColor(0x404FC3F7);
        quad(610 * sx, horizon, 622 * sx, horizon, 330 * sx, h, 100 * sx, h);
        c.drawPath(path, p);
        quad(658 * sx, horizon, 670 * sx, horizon, 1180 * sx, h, 950 * sx, h);
        c.drawPath(path, p);
        p.setColor(0x8CFFFFFF);
        for (int i = 0; i < 8; i++) {
            float y = (420 + i * i * 5.2f) * sy, dw = (3 + i * 1.3f) * sx, dh = (8 + i * 4) * sy;
            c.drawRect(w / 2 - dw / 2, y, w / 2 + dw / 2, y + dh, p);
        }
        p.setColor(0xFF000000);
        p.setShader(new LinearGradient(0, 0, w, 0, new int[]{0x73000000, 0x1A000000, 0x59000000},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    private void drawDay(Canvas c, float w, float h) {
        float sx = w / 1280f, sy = h / 720f, horizon = 420 * sy;
        p.setColor(0xFF000000);
        p.setShader(new LinearGradient(0, 0, 0, h, new int[]{0xFF3F93E0, 0xFF9FD2F5, 0xFFE6F5FF},
                new float[]{0f, .6f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        Random rnd = new Random(7);
        p.setColor(0x8C8FB9DA);
        for (int i = 0; i < 12; i++) {
            float x = (i * 115 - 50) * sx, mh = (40 + rnd.nextFloat() * 60) * sy;
            tri(x, horizon, x + 70 * sx, horizon - mh, x + 150 * sx, horizon);
            c.drawPath(path, p);
        }
        p.setColor(0x8CFFFFFF);
        float[][] clouds = {{130, 60, 90}, {360, 38, 60}, {1090, 48, 95}, {800, 34, 45}};
        for (float[] cl : clouds) {
            float x = cl[0] * sx, y = cl[1] * sy, cw = cl[2] * sx;
            c.drawOval(x - cw, y - cw * .24f, x + cw, y + cw * .24f, p);
            c.drawOval(x - cw * .95f, y - cw * .12f, x - cw * .0f, y + cw * .24f, p);
            c.drawOval(x - cw * .05f, y - cw * .10f, x + cw * .95f, y + cw * .24f, p);
        }
        p.setColor(0xFF000000);
        p.setShader(new LinearGradient(0, horizon, 0, h, 0xFF8FC3A1, 0xFF5F9A78, Shader.TileMode.CLAMP));
        c.drawRect(0, horizon, w, h, p);
        p.setShader(null);
        p.setColor(0xFF6D7888);
        quad(612 * sx, horizon, 668 * sx, horizon, 1180 * sx, h, 100 * sx, h);
        c.drawPath(path, p);
        p.setColor(0x4DFFFFFF);
        quad(612 * sx, horizon, 622 * sx, horizon, 330 * sx, h, 100 * sx, h);
        c.drawPath(path, p);
        quad(658 * sx, horizon, 668 * sx, horizon, 1180 * sx, h, 950 * sx, h);
        c.drawPath(path, p);
        p.setColor(0xBFFFFFFF);
        for (int i = 0; i < 8; i++) {
            float y = (440 + i * i * 4.6f) * sy, dw = (3 + i * 1.3f) * sx, dh = (8 + i * 4) * sy;
            c.drawRect(w / 2 - dw / 2, y, w / 2 + dw / 2, y + dh, p);
        }
        p.setColor(0xFF000000);
        p.setShader(new LinearGradient(0, 0, w, 0, new int[]{0x2EFFFFFF, 0x00FFFFFF, 0x1FFFFFFF},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    private void tri(float x1, float y1, float x2, float y2, float x3, float y3) {
        path.rewind();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
    }

    private void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        path.rewind();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x4, y4);
        path.close();
    }
}
