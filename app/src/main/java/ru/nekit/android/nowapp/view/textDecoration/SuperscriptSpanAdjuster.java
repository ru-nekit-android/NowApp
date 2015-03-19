package ru.nekit.android.nowapp.view.textDecoration;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;

/**
 * Created by chuvac on 20.03.15.
 */

public class SuperscriptSpanAdjuster extends TextAppearanceSpan {

    private int top;

    public SuperscriptSpanAdjuster(Context context, int idResource, int top) {
        super(context, idResource);
        this.top = top;
    }

    @Override
    public void updateDrawState(TextPaint paint) {
        super.updateDrawState(paint);
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        paint.baselineShift = -top - fontMetrics.ascent - fontMetrics.descent;

    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        super.updateMeasureState(paint);
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        paint.baselineShift = -top - fontMetrics.ascent - fontMetrics.descent;
    }
}

