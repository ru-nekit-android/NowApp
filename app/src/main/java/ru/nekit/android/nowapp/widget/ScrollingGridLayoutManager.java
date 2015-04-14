package ru.nekit.android.nowapp.widget;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by chuvac on 14.04.15.
 */
public class ScrollingGridLayoutManager extends GridLayoutManager {
    private final int duration;

    public ScrollingGridLayoutManager(Context context, int spanCount, int duration) {
        super(context, spanCount);
        this.duration = duration;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                       int position) {
        View firstVisibleChild = recyclerView.getChildAt(0);
        int itemHeight = firstVisibleChild.getHeight();
        int currentPosition = recyclerView.getChildAdapterPosition(firstVisibleChild);
        int distanceInPixels = Math.abs((currentPosition - position) * itemHeight);
        if (distanceInPixels == 0) {
            distanceInPixels = (int) Math.abs(firstVisibleChild.getY());
        }
        SmoothScroller smoothScroller = new SmoothScroller(recyclerView.getContext(), distanceInPixels, duration);
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    private class SmoothScroller extends LinearSmoothScroller {
        private final float distanceInPixels;
        private final float duration;

        public SmoothScroller(Context context, int distanceInPixels, int duration) {
            super(context);
            this.distanceInPixels = distanceInPixels;
            this.duration = duration;
        }

        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return ScrollingGridLayoutManager.this.computeScrollVectorForPosition(targetPosition);
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            float proportion = (float) dx / distanceInPixels;
            return (int) (duration * proportion);
        }
    }
}
