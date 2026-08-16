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

public class DonutChartView extends View {

    private static final int COLOR_ACTIVE = Color.rgb(34, 160, 107);
    private static final int COLOR_MAINTENANCE = Color.rgb(255, 183, 3);
    private static final int COLOR_INACTIVE = Color.rgb(237, 27, 47);

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] values = new float[3];
    private final int[] colors = {
            COLOR_ACTIVE,
            COLOR_MAINTENANCE,
            COLOR_INACTIVE
    };

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        textPaint.setTextAlign(Paint.Align.CENTER);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setData(float active, float maintenance, float inactive) {
        values[0] = Math.max(0f, active);
        values[1] = Math.max(0f, maintenance);
        values[2] = Math.max(0f, inactive);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float total = values[0] + values[1] + values[2];
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float strokeWidth = Math.max(dp(24), Math.min(getWidth(), getHeight()) * 0.14f);
        float radius = Math.max(
                0f,
                Math.min(getWidth(), getHeight()) / 2f - strokeWidth / 2f - dp(12)
        );

        arcPaint.setStrokeWidth(strokeWidth);
        RectF bounds = new RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
        );

        if (total <= 0f) {
            arcPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_outline));
            canvas.drawArc(bounds, 0f, 360f, false, arcPaint);
        } else {
            float startAngle = -90f;
            float gap = total > 1f ? 2f : 0f;
            for (int index = 0; index < values.length; index++) {
                if (values[index] <= 0f) {
                    continue;
                }

                float sweepAngle = values[index] / total * 360f;
                arcPaint.setColor(colors[index]);
                canvas.drawArc(
                        bounds,
                        startAngle + gap / 2f,
                        Math.max(0f, sweepAngle - gap),
                        false,
                        arcPaint
                );
                startAngle += sweepAngle;
            }
        }

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text));
        textPaint.setTextSize(sp(28));
        textPaint.setFakeBoldText(true);
        canvas.drawText(
                String.format(Locale.getDefault(), "%.0f", total),
                centerX,
                centerY + dp(2),
                textPaint
        );

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.haui_text_muted));
        textPaint.setTextSize(sp(12));
        textPaint.setFakeBoldText(false);
        canvas.drawText(
                getContext().getString(R.string.report_rooms_unit),
                centerX,
                centerY + dp(23),
                textPaint
        );
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}