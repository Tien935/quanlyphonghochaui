package com.example.phonghochaui.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.phonghochaui.R;
import com.example.phonghochaui.data.model.DashboardStatistics;

import java.util.ArrayList;
import java.util.List;

public class LineChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<DashboardStatistics.TrendPoint> points = new ArrayList<>();

    public LineChartView(Context context) {
        super(context);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChartView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
    }

    public void setData(
            List<DashboardStatistics.TrendPoint> source,
            int days
    ) {
        points.clear();
        if (source != null && !source.isEmpty()) {
            int safeDays = Math.max(1, days);
            int start = Math.max(0, source.size() - safeDays);
            points.addAll(source.subList(start, source.size()));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text_muted));
            textPaint.setTextSize(sp(13));
            canvas.drawText(
                    getContext().getString(R.string.report_no_chart_data),
                    getWidth() / 2f,
                    getHeight() / 2f,
                    textPaint
            );
            return;
        }

        float left = dp(42);
        float top = dp(26);
        float right = getWidth() - dp(12);
        float bottom = getHeight() - dp(42);
        float chartWidth = Math.max(0f, right - left);
        float chartHeight = Math.max(0f, bottom - top);

        long maxValue = 0L;
        for (DashboardStatistics.TrendPoint point : points) {
            maxValue = Math.max(maxValue, point.getTotal());
        }

        long gridStep = Math.max(1L, (long) Math.ceil(maxValue / 4.0));
        long axisMax = Math.max(4L, gridStep * 4L);

        paint.setStrokeWidth(dp(1));
        textPaint.setTextSize(sp(10));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text_muted));
        textPaint.setTextAlign(Paint.Align.RIGHT);

        for (int line = 0; line <= 4; line++) {
            float y = bottom - chartHeight * line / 4f;
            paint.setColor(ContextCompat.getColor(getContext(), R.color.haui_outline));
            canvas.drawLine(left, y, right, y, paint);
            canvas.drawText(
                    String.valueOf(axisMax * line / 4L),
                    left - dp(7),
                    y + dp(4),
                    textPaint
            );
        }

        float denominator = Math.max(1, points.size() - 1);
        Path linePath = new Path();
        Path fillPath = new Path();

        for (int index = 0; index < points.size(); index++) {
            float x = left + chartWidth * index / denominator;
            float y = bottom - chartHeight * points.get(index).getTotal() / axisMax;
            if (index == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        fillPath.lineTo(right, bottom);
        fillPath.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(
                0f,
                top,
                0f,
                bottom,
                Color.argb(90, 13, 65, 120),
                Color.argb(5, 13, 65, 120),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2.5f));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.haui_blue));
        canvas.drawPath(linePath, paint);

        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int index = 0; index < points.size(); index++) {
            float x = left + chartWidth * index / denominator;
            float y = bottom - chartHeight * points.get(index).getTotal() / axisMax;

            paint.setColor(ContextCompat.getColor(getContext(), R.color.haui_blue));
            canvas.drawCircle(x, y, points.size() <= 7 ? dp(4) : dp(2.2f), paint);

            if (points.size() <= 7) {
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text));
                textPaint.setTextSize(sp(10));
                textPaint.setFakeBoldText(true);
                canvas.drawText(
                        String.valueOf(points.get(index).getTotal()),
                        x,
                        Math.max(top + dp(9), y - dp(8)),
                        textPaint
                );
            }
        }

        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(10));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text_muted));
        drawDateLabel(canvas, 0, left, bottom);

        if (points.size() > 2) {
            int middle = points.size() / 2;
            float middleX = left + chartWidth * middle / denominator;
            drawDateLabel(canvas, middle, middleX, bottom);
        }

        if (points.size() > 1) {
            drawDateLabel(canvas, points.size() - 1, right, bottom);
        }
    }

    private void drawDateLabel(Canvas canvas, int index, float x, float bottom) {
        canvas.drawText(shortDate(points.get(index).getDate()), x, bottom + dp(20), textPaint);
    }

    private String shortDate(String value) {
        if (value == null || value.length() < 10) {
            return value == null ? "" : value;
        }
        return value.substring(8, 10) + "/" + value.substring(5, 7);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}