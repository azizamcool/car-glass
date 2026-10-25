package my.carglass;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Frosted glass: draws the matching slice of the (pre-blurred) background behind the view, then a translucent
 * gradient, a border and a bright top edge. Android 11 has no live backdrop blur, so the blur comes from BackgroundView.
 */
final class GlassDrawable extends Drawable {
    private final BackgroundView bg;
    private final View host;
    private final float radius;
    private final boolean circle, light, highlight;
    private final int accent;

    private final Paint photo = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final RectF rect = new RectF();
    private final Rect src = new Rect();
    private final int[] a = new int[2], b = new int[2];
    private boolean pressed;

    GlassDrawable(BackgroundView bg, View host, float radiusPx, boolean circle, boolean light, boolean highlight, int accent) {
        this.bg = bg;
        this.host = host;
        this.radius = radiusPx;
        this.circle = circle;
        this.light = light;
        this.highlight = highlight;
        this.accent = accent;
        line.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onBoundsChange(Rect r) {
        rect.set(r);
        rect.inset(.75f, .75f);
        shape.rewind();
        if (circle) shape.addOval(rect, Path.Direction.CW);
        else shape.addRoundRect(rect, radius, radius, Path.Direction.CW);
        int top = light ? (highlight ? 0x8CFFFFFF : 0x42FFFFFF) : (highlight ? 0x38FFFFFF : 0x1CFFFFFF);
        int bottom = light ? (highlight ? 0x2EFFFFFF : 0x0FFFFFFF) : (highlight ? 0x0FFFFFFF : 0x06FFFFFF);
        fill.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom, top, bottom, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas c) {
        if (rect.isEmpty()) return;
        Bitmap blur = bg.getBlurred();
        if (blur != null && bg.getWidth() > 0) {
            host.getLocationInWindow(a);
            bg.getLocationInWindow(b);
            Rect bounds = getBounds();
            float sx = blur.getWidth() / (float) bg.getWidth(), sy = blur.getHeight() / (float) bg.getHeight();
            float left = (a[0] - b[0] + bounds.left) * sx, top = (a[1] - b[1] + bounds.top) * sy;
            src.set(Math.round(left), Math.round(top), Math.round(left + bounds.width() * sx), Math.round(top + bounds.height() * sy));
            c.save();
            c.clipPath(shape);
            c.drawBitmap(blur, src, bounds, photo);
            c.restore();
        }
        c.drawPath(shape, fill);
        if (pressed) {
            fill.setShader(null);
            fill.setColor((accent & 0x00FFFFFF) | 0x33000000);
            c.drawPath(shape, fill);
            onBoundsChange(getBounds());
        }
        float w = Math.max(1f, host.getResources().getDisplayMetrics().density);
        line.setStrokeWidth(w);
        line.setColor(pressed || highlight ? accent : light ? 0x9EFFFFFF : 0x2EFFFFFF);
        c.drawPath(shape, line);
        // bright top edge
        line.setShader(new LinearGradient(0, rect.top, 0, rect.top + rect.height() * .5f,
                light ? 0xE6FFFFFF : 0x4DFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        line.setStrokeWidth(w * 1.6f);
        c.save();
        c.translate(0, w);
        c.drawPath(shape, line);
        c.restore();
        line.setShader(null);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean p = false;
        for (int s : state) {
            if (s == android.R.attr.state_pressed || s == android.R.attr.state_focused) p = true;
        }
        if (p != pressed) {
            pressed = p;
            invalidateSelf();
            return true;
        }
        return false;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
