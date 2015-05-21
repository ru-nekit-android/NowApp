package ru.nekit.android.nowapp.widget;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

import ru.nekit.android.nowapp.R;

/**
 * Created by chuvac on 20.05.15.
 */
public class FloatingActionButtonForRecyclerViewScrollAnimator {

    private boolean mVisible;

    private static final int TRANSLATE_DURATION_MILLIS = 200;


    private FloatingActionButton mButton;
    private RecyclerViewScrollDetectorImpl mScrollDetector;
    private RecyclerView mRecyclerView;
    private int mScrollThreshold;


    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public FloatingActionButtonForRecyclerViewScrollAnimator(@NonNull Context context, @NonNull FloatingActionButton button, @NonNull RecyclerView recyclerView) {
        mButton = button;
        mVisible = true;
        mScrollThreshold = context.getResources().getDimensionPixelOffset(R.dimen.fab_scroll_threshold);
        mRecyclerView = recyclerView;
    }

    private void attachToRecyclerView(@NonNull RecyclerView recyclerView,
                                      ScrollDirectionListener scrollDirectionlistener,
                                      RecyclerView.OnScrollListener onScrollListener) {
        mScrollDetector = new RecyclerViewScrollDetectorImpl();
        mScrollDetector.setScrollDirectionListener(scrollDirectionlistener);
        mScrollDetector.setOnScrollListener(onScrollListener);

        mScrollDetector.setScrollThreshold(mScrollThreshold);
        recyclerView.addOnScrollListener(mScrollDetector);
    }

    public void dettachFromRecyclerView() {
        if (mScrollDetector != null) {
            mRecyclerView.removeOnScrollListener(mScrollDetector);
        }
    }

    public void attachToRecyclerView() {
        attachToRecyclerView(mRecyclerView, null, null);
    }


    abstract class RecyclerViewScrollDetector extends RecyclerView.OnScrollListener {
        private int mScrollThreshold;

        abstract void onScrollUp();

        abstract void onScrollDown();

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            boolean isSignificantDelta = Math.abs(dy) > mScrollThreshold;
            if (isSignificantDelta) {
                if (dy > 0) {
                    onScrollUp();
                } else {
                    onScrollDown();
                }
            }
        }

        public void setScrollThreshold(int scrollThreshold) {
            mScrollThreshold = scrollThreshold;
        }
    }

    public interface ScrollDirectionListener {
        void onScrollDown();

        void onScrollUp();
    }

    private class RecyclerViewScrollDetectorImpl extends RecyclerViewScrollDetector {
        private ScrollDirectionListener mScrollDirectionListener;
        private RecyclerView.OnScrollListener mOnScrollListener;

        private void setScrollDirectionListener(ScrollDirectionListener scrollDirectionListener) {
            mScrollDirectionListener = scrollDirectionListener;
        }

        public void setOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
            mOnScrollListener = onScrollListener;
        }

        @Override
        public void onScrollDown() {
            show();
            if (mScrollDirectionListener != null) {
                mScrollDirectionListener.onScrollDown();
            }
        }

        @Override
        public void onScrollUp() {
            hide();
            if (mScrollDirectionListener != null) {
                mScrollDirectionListener.onScrollUp();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (mOnScrollListener != null) {
                mOnScrollListener.onScrolled(recyclerView, dx, dy);
            }

            super.onScrolled(recyclerView, dx, dy);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (mOnScrollListener != null) {
                mOnScrollListener.onScrollStateChanged(recyclerView, newState);
            }

            super.onScrollStateChanged(recyclerView, newState);
        }
    }

    public void show() {
        show(true);
    }

    public void hide() {
        hide(true);
    }

    public void show(boolean animate) {
        toggle(true, animate, false);
    }

    public void hide(boolean animate) {
        toggle(false, animate, false);
    }

    private void toggle(final boolean visible, final boolean animate, boolean force) {
        if (mVisible != visible || force) {
            mVisible = visible;
            int height = mButton.getHeight();
            if (height == 0 && !force) {
                ViewTreeObserver vto = mButton.getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            ViewTreeObserver currentVto = mButton.getViewTreeObserver();
                            if (currentVto.isAlive()) {
                                currentVto.removeOnPreDrawListener(this);
                            }
                            toggle(visible, animate, true);
                            return true;
                        }
                    });
                    return;
                }
            }

            int translationY = visible ? 0 : height + getMarginBottom();
            if (animate) {
                ViewPropertyAnimator.animate(mButton).setInterpolator(mInterpolator)
                        .setDuration(TRANSLATE_DURATION_MILLIS)
                        .translationY(translationY);
            } else {
                ViewHelper.setTranslationY(mButton, translationY);
            }

            // On pre-Honeycomb a translated view is still clickable, so we need to disable clicks manually
            if (!hasHoneycombApi()) {
                mButton.setClickable(visible);
            }
        }
    }

    private int getMarginBottom() {

        int marginBottom = 0;
        final ViewGroup.LayoutParams layoutParams = mButton.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            marginBottom = ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
        }
        return marginBottom;
    }

    private boolean hasHoneycombApi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

}
