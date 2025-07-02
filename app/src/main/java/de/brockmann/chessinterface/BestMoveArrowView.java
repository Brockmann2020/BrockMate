package de.brockmann.chessinterface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BestMoveArrowView extends View {
    private float startX, startY, endX, endY;
    private boolean showArrow = false;
    private final Paint paint = new Paint();

    public BestMoveArrowView(Context context) {
        super(context);
        init();
    }

    public BestMoveArrowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BestMoveArrowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
    }

    public void setArrow(float sx, float sy, float ex, float ey) {
        startX = sx;
        startY = sy;
        endX = ex;
        endY = ey;
        showArrow = true;
        invalidate();
    }

    public void clearArrow() {
        showArrow = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!showArrow) return;
        canvas.drawLine(startX, startY, endX, endY, paint);
        float dx = endX - startX;
        float dy = endY - startY;
        double angle = Math.atan2(dy, dx);
        float len = 30f;
        float x1 = endX - (float) (len * Math.cos(angle - Math.PI / 6));
        float y1 = endY - (float) (len * Math.sin(angle - Math.PI / 6));
        float x2 = endX - (float) (len * Math.cos(angle + Math.PI / 6));
        float y2 = endY - (float) (len * Math.sin(angle + Math.PI / 6));
        canvas.drawLine(endX, endY, x1, y1, paint);
        canvas.drawLine(endX, endY, x2, y2, paint);
    }
}
