package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItemsLoader;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.modelView.EventCollectionAdapter;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemSelectListener;

public class EventCollectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Void> {

    private static final int LOADER_ID = 1;
    public static final String TAG = "ru.nekit.android.event_collection_fragment";

    enum LOADING_TYPES {
        PULL_TO_REFRESH,
        REQUEST_NEW_EVENT_ITEMS
    }

    enum LOADING_STATE {
        LOADING, LOADED
    }

    private RecyclerView mEventCollectionList;
    private EventCollectionAdapter mEventCollectionAdapter;
    private GridLayoutManager mEventCollectionLayoutManager;
    private IEventItemSelectListener mEventItemSelectListener;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private EventItemsModel mEventModel;

    private LOADING_STATE mState = LOADING_STATE.LOADED;
    private LOADING_TYPES mLoadingType = LOADING_TYPES.PULL_TO_REFRESH;


    public EventCollectionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mEventModel = ((NowApplication) getActivity().getApplication()).getEventModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context context = getActivity();
        View view = inflater.inflate(R.layout.fragment_event_collection, container, false);
        mEventCollectionList = (RecyclerView) view.findViewById(R.id.event_collection_list);
        mEventCollectionList.setHasFixedSize(true);
        mEventCollectionList.setItemAnimator(new DefaultItemAnimator());
        int listColumn = getResources().getInteger(R.integer.event_collection_column_count);

        mEventCollectionLayoutManager = new GridLayoutManager(context, listColumn);
        mEventCollectionAdapter = new EventCollectionAdapter(context, mEventModel.getEventItemsList(), listColumn);
        mEventCollectionList.setAdapter(mEventCollectionAdapter);
        mEventCollectionList.setLayoutManager(mEventCollectionLayoutManager);
        mEventCollectionAdapter.setOnItemClickListener(mEventItemSelectListener);
        mEventCollectionLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mEventCollectionAdapter.getSpanSize(position);
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mLoadingType = LOADING_TYPES.PULL_TO_REFRESH;
                performLoad();
            }
        });

        mEventCollectionList.setOnScrollListener(mScrollListener);
        mEventCollectionAdapter.setItems(mEventModel.getEventItemsList());
        return view;
    }

    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            int totalItemCount = mEventCollectionLayoutManager.getItemCount();
            int lastVisibleItem = mEventCollectionLayoutManager.findLastVisibleItemPosition();

            if (totalItemCount > 1) {
                if (lastVisibleItem >= totalItemCount - 1) {
                    mLoadingType = LOADING_TYPES.REQUEST_NEW_EVENT_ITEMS;
                    setState(LOADING_STATE.LOADING);
                }
            }
        }
    };

    private void setState(LOADING_STATE state) {
        if (mState == state) return;
        mState = state;
        switch (mState) {
            case LOADING:
                if (!mEventCollectionAdapter.isLoading()) {
                    mEventCollectionAdapter.addLoading();
                }
                mEventCollectionList.smoothScrollToPosition(mEventModel.getEventItemsList().size());
                performLoad();
                break;
            case LOADED:
                if (mEventCollectionAdapter.isLoading()) {
                    mEventCollectionAdapter.removeLoading();
                }
                break;
        }
    }

    private void performLoad() {
        LoaderManager loaderManager = getLoaderManager();
        Bundle loaderArgs = new Bundle();
        String type = null;
        if (mLoadingType == LOADING_TYPES.PULL_TO_REFRESH) {
            type = EventItemsModel.REFRESH_EVENT_ITEMS;
        } else if (mLoadingType == LOADING_TYPES.REQUEST_NEW_EVENT_ITEMS) {
            type = EventItemsModel.REQUEST_NEW_EVENT_ITEMS;
        }
        loaderArgs.putString(EventItemsModel.TYPE, type);
        final Loader<Void> loader = loaderManager.getLoader(LOADER_ID);
        if (loader != null) {
            loaderManager.restartLoader(LOADER_ID, loaderArgs, EventCollectionFragment.this);
        } else {
            loaderManager.initLoader(LOADER_ID, loaderArgs, EventCollectionFragment.this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEventItemSelectListener = (IEventItemSelectListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSplashScreenCompleteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEventItemSelectListener = null;
    }

    @Override
    public void onPause() {

        super.onPause();
    }

    @Override
    public Loader<Void> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            EventItemsLoader loader = new EventItemsLoader(getActivity(), args);
            loader.forceLoad();
            return loader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Void> loader, Void data) {
        setState(LOADING_STATE.LOADED);
        mLoadingType = LOADING_TYPES.PULL_TO_REFRESH;
        mEventCollectionAdapter.setItems(mEventModel.getEventItemsList());
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<Void> loader) {
        setState(LOADING_STATE.LOADED);
        mLoadingType = LOADING_TYPES.PULL_TO_REFRESH;
        mEventCollectionAdapter.setItems(null);
        mSwipeRefreshLayout.setRefreshing(false);
    }
}
