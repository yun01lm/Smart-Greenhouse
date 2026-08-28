package com.greenhouse.app.ui.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * 迷你趋势曲线（Sparkline）：零依赖 Canvas 绘制
 * <p>UI 重构方向 A / F1 数据可视化：看板传感器卡片内嵌近期波动曲线。</p>
 */
public class SparklineView extends View {

    private float[] values = new float[0];
    private int lineColor = Color.parseColor("#3DDC84");

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SparklineView(Context context) {
        super(context);
        init();
    }

    public SparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(lineColor);
        fillPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(lineColor);
    }

    public void setValues(float[] v) {
        values = (v == null) ? new float[0] : v;
        invalidate();
    }

    public void setLineColor(int color) {
        lineColor = color;
        linePaint.setColor(color);
        dotPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || values.length < 2) return;

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (max - min < 1e-6f) {
            max = min + 1f;
            min -= 1f;
        }

        float pad = 6f;
        float stepX = (w - pad * 2) / (values.length - 1);
        Path path = new Path();
        Path fill = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = pad + i * stepX;
            float y = h - pad - (values[i] - min) / (max - min) * (h - pad * 2);
            if (i == 0) {
                path.moveTo(x, y);
                fill.moveTo(x, y);
            } else {
                path.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }

        // 渐变填充（线色 30% → 透明）
        fill.lineTo(pad + (values.length - 1) * stepX, h);
        fill.lineTo(pad, h);
        fill.close();
        int fillBase = (lineColor & 0x00FFFFFF) | 0x4D000000;
        fillPaint.setShader(new LinearGradient(0, 0, 0, h, fillBase, Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawPath(fill, fillPaint);
        canvas.drawPath(path, linePaint);

        // 最后一点高亮
        float lx = pad + (values.length - 1) * stepX;
        float ly = h - pad - (values[values.length - 1] - min) / (max - min) * (h - pad * 2);
        canvas.drawCircle(lx, ly, 4f, dotPaint);
    }
}
