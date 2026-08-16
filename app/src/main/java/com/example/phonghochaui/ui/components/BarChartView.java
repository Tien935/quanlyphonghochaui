package com.example.phonghochaui.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.phonghochaui.R;

import java.util.Locale;

public class BarChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long[] values = new long[4];
    private final String[] labels = {"", "", "", ""};
    private final int[] colors = {
            Color.rgb(255, 183, 3),
            Color.rgb(34, 160, 107),
            Color.rgb(237, 27, 47),
            Color.rgb(152, 162, 179)
    };

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.NORMAL
        ));
    }

    public void setData(long[] sourceValues, String[] sourceLabels) {
        for (int index = 0; index < values.length; index++) {
            values[index] = sourceValues != null && index < sourceValues.length
                    ? Math.max(0L, sourceValues[index])
                    : 0L;
            labels[index] = sourceLabels != null && index < sourceLabels.length
                    ? sourceLabels[index]
                    : "";
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = dp(42);
        float top = dp(30);
        float right = getWidth() - dp(10);
        float bottom = getHeight() - dp(43);
        float chartWidth = Math.max(0f, right - left);
        float chartHeight = Math.max(0f, bottom - top);

        long maxValue = 0L;
        for (long value : values) {
            maxValue = Math.max(maxValue, value);
        }

        long gridStep = Math.max(1L, (long) Math.ceil(maxValue / 4.0));
        long axisMax = Math.max(4L, gridStep * 4L);

        paint.setStrokeWidth(dp(1));
        textPaint.setTextSize(sp(10));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text_muted));

        for (int line = 0; line <= 4; line++) {
            float y = bottom - chartHeight * line / 4f;
            paint.setColor(ContextCompat.getColor(getContext(), R.color.haui_outline));
            canvas.drawLine(left, y, right, y, paint);

            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(
                    String.valueOf(axisMax * line / 4L),
                    left - dp(7),
                    y + dp(4),
                    textPaint
            );
        }

        float slotWidth = chartWidth / values.length;
        float barWidth = Math.min(dp(48), slotWidth * 0.56f);

        for (int index = 0; index < values.length; index++) {
            float centerX = left + slotWidth * (index + 0.5f);
            float barHeight = chartHeight * values[index] / axisMax;
            float barTop = bottom - barHeight;

            paint.setColor(colors[index]);
            paint.setStyle(Paint.Style.FILL);
            RectF bar = new RectF(
                    centerX - barWidth / 2f,
                    barTop,
                    centerX + barWidth / 2f,
                    bottom
            );
            canvas.drawRoundRect(bar, dp(5), dp(5), paint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text));
            textPaint.setTextSize(sp(11));
            textPaint.setFakeBoldText(true);
            canvas.drawText(
                    String.format(Locale.getDefault(), "%d", values[index]),
                    centerX,
                    Math.max(top + dp(10), barTop - dp(7)),
                    textPaint
            );

            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text_muted));
            textPaint.setTextSize(sp(10));
            textPaint.setFakeBoldText(false);
            canvas.drawText(labels[index], centerX, bottom + dp(20), textPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}