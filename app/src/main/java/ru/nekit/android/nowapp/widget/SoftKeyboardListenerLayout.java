package ru.nekit.android.nowapp.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created by chuvac on 22.05.15.
 */

public class SoftKeyboardListenerLayout extends RelativeLayout {


    public SoftKeyboardListenerLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SoftKeyboardListenerLayout(Context context) {
        super(context);
    }

    private OnSoftKeyboardListener onSoftKeyboardListener;

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


