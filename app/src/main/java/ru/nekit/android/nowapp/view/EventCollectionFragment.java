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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsLoader;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.modelView.EventCollectionAdapter;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemSelectListener;
import ru.nekit.android.nowapp.widget.ScrollingGridLayoutManager;

import static ru.nekit.android.nowapp.NowApplication.STATE.ONLINE;

public class EventCollectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Integer>, IEventItemSelectListener, View.OnClickListener {

    public static final String TAG = "ru.nekit.android.event_collection_fragment";

    private static final int LOADER_ID = 2;
    private static final int SMOOTH_SCROLL_DURATION = 1000;


    enum LOADING_STATE {
        LOADING, LOADED
    }

    private RecyclerView mEventItemsView;
    private EventCollectionAdapter mEventCollectionAdapter;
    private ScrollingGridLayoutManager mEventCollectionLayoutManager;
    private IEventItemSelectListener mEventItemSelectListener;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private EventItemsModel mEventModel;
    private int mCurrentPage;

    private LOADING_STATE mLoadingState = LOADING_STATE.LOADED;
    private String mLoadingType = EventItemsModel.REFRESH_EVENT_ITEMS;

    public EventCollectionFragment() {
    }

    @Override
    public void onEventItemSelect(EventItem eventItem) {
        if (mLoadingState == LOADING_STATE.LOADED) {
            mEventItemSelectListener.onEventItemSelect(eventItem);
        } else {
            //strange behavior on usual user-case
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mEventModel = ((NowApplication) getActivity().getApplication()).getEventModel();
        mCurrentPage = mEventModel.getCurrentPage();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        if (!mEventModel.isEventItemsListEmpty()) {
            if (mEventModel.getCurrentPage() != mCurrentPage) {
                mEventCollectionAdapter.setItems(mEventModel.getEventItems());
                mCurrentPage = mEventModel.getCurrentPage();
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mEventModel.isEventItemsListEmpty()) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
            mLoadingType = EventItemsModel.REFRESH_EVENT_ITEMS;
            performLoad();
        }
        View view = getView();
        if (view != null) {
            view.getRootView().findViewById(R.id.now_title).setOnClickListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();
        View view = inflater.inflate(R.layout.fragment_event_collection, container, false);
        mEventItemsView = (RecyclerView) view.findViewById(R.id.event_collection_list);
        mEventItemsView.setHasFixedSize(true);
        mEventItemsView.setItemAnimator(new DefaultItemAnimator());
        int listColumn = getResources().getInteger(R.integer.event_collection_column_count);

        mEventCollectionLayoutManager = new ScrollingGridLayoutManager(context, listColumn, SMOOTH_SCROLL_DURATION);
        mEventCollectionAdapter = new EventCollectionAdapter(context, mEventModel, listColumn);
        mEventCollectionAdapter.setHasStableIds(true);
        mEventItemsView.setAdapter(mEventCollectionAdapter);
        mEventItemsView.setLayoutManager(mEventCollectionLayoutManager);
        mEventCollectionAdapter.setOnItemClickListener(this);
        mEventCollectionLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mEventCollectionAdapter.getSpanSize(position);
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        mSwipeRefreshLayout.setEnabled(NowApplication.getState() == ONLINE);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mLoadingType = EventItemsModel.REFRESH_EVENT_ITEMS;
                setLoadingState(LOADING_STATE.LOADING);
                performLoad();
            }
        });

        mEventCollectionAdapter.setLoadMoreListener(new EventCollectionAdapter.OnLoadMorelListener() {
            @Override
            public void onLoadMore() {
                mLoadingType = EventItemsModel.REQUEST_NEW_EVENT_ITEMS;
                setLoadingState(LOADING_STATE.LOADING);
            }
        });

        return view;
    }

    private void setLoadingState(LOADING_STATE state) {
        if (mLoadingState == state) return;
        mLoadingState = state;
        if (EventItemsModel.REQUEST_NEW_EVENT_ITEMS.equals(mLoadingType)) {
            switch (mLoadingState) {
                case LOADING:
                    if (!mEventCollectionAdapter.isLoading()) {
                        mEventCollectionAdapter.addLoading();
                    }
                    performLoad();
                    break;
                case LOADED:
                    if (mEventCollectionAdapter.isLoading()) {
                        mEventCollectionAdapter.removeLoading();
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private void performLoad() {
        LoaderManager loaderManager = getLoaderManager();
        Bundle loaderArgs = new Bundle();
        loaderArgs.putString(EventItemsModel.LOADING_TYPE, mLoadingType);
        final Loader<Integer> loader = loaderManager.getLoader(LOADER_ID);
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
        } catch (ClassCastException exp) {
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
        mSwipeRefreshLayout.setRefreshing(false);
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.destroyLoader(LOADER_ID);
        setLoadingState(LOADING_STATE.LOADED);
        super.onPause();
    }

    @Override
    public Loader<Integer> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            EventItemsLoader loader = new EventItemsLoader(getActivity(), args);
            loader.forceLoad();
            return loader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Integer> loader, Integer result) {
        if (isResumed()) {
            if (result == EventItemsLoader.RESULT_OK) {
                if (EventItemsModel.REFRESH_EVENT_ITEMS.equals(mLoadingType)) {
                    mEventCollectionAdapter.setItems(mEventModel.getEventItems());
                } else if (EventItemsModel.REQUEST_NEW_EVENT_ITEMS.equals(mLoadingType)) {
                    mEventCollectionAdapter.addItems(mEventModel.getLastAddedEventItems());
                }
                mCurrentPage = mEventModel.getCurrentPage();
            } else {
                Toast.makeText(getActivity(), Html.fromHtml(getResources().getString(R.string.error_while_data_loading)), Toast.LENGTH_LONG).show();
            }
            setLoadingState(LOADING_STATE.LOADED);
            mLoadingType = EventItemsModel.REFRESH_EVENT_ITEMS;
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Integer> loader) {
        setLoadingState(LOADING_STATE.LOADED);
        mLoadingType = EventItemsModel.REFRESH_EVENT_ITEMS;
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.now_title:
                if (isResumed()) {
                    mEventItemsView.smoothScrollToPosition(0);
                }
                break;
            default:
                break;
        }
    }
}
