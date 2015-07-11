package ru.nekit.android.nowapp.utils;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.SuperscriptSpan;

/**
 * Created by chuvac on 20.03.15.
 */
public class TextViewUtils {

    @NonNull
    private static Rect getTextBounds(@NonNull String text, float textSize, Typeface typeface) {
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }

    @NonNull
    public static SuperscriptSpanAdjuster getSuperscriptSpanAdjuster(@NonNull Context context, @NonNull String baseText, int baseTextSize) {
        float scaledDensity = context.getResources().getDisplayMetrics().density;
        if (scaledDensity == 1.5) {
            scaledDensity *= 4;
        } else if (scaledDensity == 2) {
            scaledDensity *= 1;
        } else if (scaledDensity == 3) {
            scaledDensity *= 2;
        } else {
            scaledDensity *= 0;
        }
        int baseHeight = TextViewUtils.getTextBounds(baseText, baseTextSize - scaledDensity, Typeface.DEFAULT).height();
        return new SuperscriptSpanAdjuster(baseHeight);
    }

    static class SuperscriptSpanAdjuster extends SuperscriptSpan {

        private int baseHeight;

        public SuperscriptSpanAdjuster(int baseHeight) {
            super();
            this.baseHeight = baseHeight;
        }

        @Override
        public void updateDrawState(@NonNull TextPaint paint) {
            super.updateDrawState(paint);
            Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
            paint.baselineShift = -baseHeight - fontMetrics.ascent - fontMetrics.descent;

        }

        @Override
        public void updateMeasureState(@NonNull TextPaint paint) {
            super.updateMeasureState(paint);
            Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
            paint.baselineShift = -baseHeight - fontMetrics.ascent - fontMetrics.descent;
        }
    }

}
