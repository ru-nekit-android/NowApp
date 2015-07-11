package ru.nekit.android.nowapp.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;

import com.devspark.robototextview.util.RobotoTextViewUtils;
import com.devspark.robototextview.util.RobotoTypefaceManager;

import ru.nekit.android.nowapp.R;

public class RobotoTextAppearanceSpan extends TextAppearanceSpan {

    private Typeface mTypeface;

    public RobotoTextAppearanceSpan(@NonNull Context context, int appearance) {
        super(context, appearance);
        TypedArray a =
                context.obtainStyledAttributes(appearance, R.styleable.RobotoTextView);
        int typeface = a.getInt(R.styleable.RobotoTextView_typeface, 0);
        a.recycle();
        mTypeface = RobotoTypefaceManager.obtainTypeface(context, typeface);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        super.updateDrawState(paint);
        RobotoTextViewUtils.setTypeface(paint, mTypeface);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        super.updateMeasureState(paint);
        RobotoTextViewUtils.setTypeface(paint, mTypeface);
    }

}