package ru.nekit.android.nowapp.modelView.listeners;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by chuvac on 15.03.15.
 */
public abstract class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener {

    private GridLayoutManager mLayoutManager;

    public EndlessRecyclerOnScrollListener(GridLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
    }

    /*public static String TAG = EndlessRecyclerOnScrollListener.class.getSimpleName();

    int mVisibleItemCount, mTotalItemCount, mFirstVisibleItem;

    private int currentPage = 1;

    private LinearLayoutManager mLayoutManager;



    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        mVisibleItemCount = mLayoutManager.getChildCount();
        mTotalItemCount = mLayoutManager.getItemCount();
        mFirstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

        if (mState == State.LOADING_PAGE) {
            if (mTotalItemCount > mPreviousTotal) {
                mPreviousTotal = mTotalItemCount;
                mPreviousTotal = mTotalItemCount = mLayoutManager.getItemCount();
                setState(State.LOADED);
            }
        }

        if (!mEndOfListReached && !(mState == State.LOADING_PAGE) && (mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem +
                mLoadingTreshold)) {
            MediaProvider.Filters filters = mFilters;
            filters.page = mPage;
            mProvider.getList(mItems, filters, MediaListFragment.this);

            mFilters = filters;

            mPreviousTotal = mTotalItemCount = mLayoutManager.getItemCount();
            setState(State.LOADING_PAGE);
        }
    }

    public void reset() {
        currentPage = 1;
        previousTotal = 0;
    }*/

    private int mLastVisibleItem;


        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            int lastVisible = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
            if (recyclerView.getAdapter().getItemCount() - 1 == lastVisible && lastVisible != mLastVisibleItem) {
                mLastVisibleItem = lastVisible;
                //if (mCallbacks != null) {
                    onLoadMore(0);
               // }
            }
        }

    public abstract void onLoadMore(int currentPage);
}