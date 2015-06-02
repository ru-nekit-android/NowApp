package ru.nekit.android.nowapp.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import ru.nekit.android.nowapp.R;

/**
 * Created by chuvac on 20.05.15.
 */
public class FloatingActionButtonAnimator {

    private static final int TRANSLATE_DURATION_MILLIS = 200;
    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private boolean mVisible;
    private View mButton;
    private RecyclerViewScrollDetectorImpl mScrollDetector;
    private RecyclerView mRecyclerView;
    private int mScrollThreshold;

    public FloatingActionButtonAnimator(@NonNull Context context, @NonNull View button, @NonNull RecyclerView recyclerView) {
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

    public void show() {
        toggle(true, false);
    }

    public void hide() {
        toggle(false, false);
    }

    private void toggle(final boolean visible, boolean force) {
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
                            toggle(visible, true);
                            return true;
                        }
                    });
                    return;
                }
            }

            int translationY = visible ? 0 : height + getMarginBottom();

            ViewPropertyAnimator an = mButton.animate();
            an.setInterpolator(mInterpolator)
                    .setDuration(TRANSLATE_DURATION_MILLIS)
                    .translationY(translationY);
            mButton.setClickable(visible);
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

    public interface ScrollDirectionListener {
        void onScrollDown();

        void onScrollUp();
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

}
