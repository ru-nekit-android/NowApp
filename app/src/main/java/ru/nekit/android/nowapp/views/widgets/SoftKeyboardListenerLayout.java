package ru.nekit.android.nowapp.views.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

/**
 * Created by chuvac on 22.05.15.
 */

public class SoftKeyboardListenerLayout extends CoordinatorLayout {


    private OnSoftKeyboardListener onSoftKeyboardListener;

    public SoftKeyboardListenerLayout(@NonNull final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SoftKeyboardListenerLayout(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (onSoftKeyboardListener != null) {
            final int newSpec = MeasureSpec.getSize(heightMeasureSpec);
            final int oldSpec = getMeasuredHeight();
            if (oldSpec >= newSpec) {
                onSoftKeyboardListener.onSoftKeyboardShown();
            } else {
                onSoftKeyboardListener.onSoftKeyboardHidden();
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public final void setOnSoftKeyboardListener(final OnSoftKeyboardListener listener) {
        this.onSoftKeyboardListener = listener;
    }

    public interface OnSoftKeyboardListener {
        void onSoftKeyboardShown();

        void onSoftKeyboardHidden();
    }
}



