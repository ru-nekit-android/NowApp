package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import in.srain.cube.views.ptr.header.MaterialHeader;
import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItemsLoader;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.modelView.listeners.EndlessRecyclerOnScrollListener;
import ru.nekit.android.nowapp.modelView.EventCollectionAdapter;
import ru.nekit.android.nowapp.modelView.decoration.GridItemDecoration;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemSelectListener;
import ru.nekit.android.nowapp.modelView.listeners.RecyclerItemClickListener;

public class EventCollectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Void> {

    private static final int LOADER_ID = 1;
    public static final String TAG = "ru.nekit.android.event_collection_fragment";

    enum LOADING_TYPES {
        PULL_TO_REFRESH,
        REQUEST_NEW_EVENT_ITEMS
    }

    private RecyclerView mEventCollectionList;
    private EventCollectionAdapter mEventCollectionAdapter;
    private GridLayoutManager mEventCollectionLayoutManager;
    private EndlessRecyclerOnScrollListener mScrollListener;
    private IEventItemSelectListener mEventItemSelectListener;
    private PtrClassicFrameLayout mRefreshFrame;
    private EventItemsModel mEventModel;
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
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.event_collection_list_spacing);
        mEventCollectionList.addItemDecoration(new GridItemDecoration(spacingInPixels, spacingInPixels));
        int listColumn = getResources().getInteger(R.integer.event_collection_column_count);

        mEventCollectionLayoutManager = new GridLayoutManager(context, listColumn);
        mEventCollectionAdapter = new EventCollectionAdapter(context);
        mEventCollectionList.setAdapter(mEventCollectionAdapter);
        mEventCollectionList.setLayoutManager(mEventCollectionLayoutManager);

        mRefreshFrame = (PtrClassicFrameLayout) view.findViewById(R.id.refresh_frame);
        mRefreshFrame.setLastUpdateTimeRelateObject(this);
        mRefreshFrame.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                performLoad();
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
            }
        });
        final MaterialHeader header = new MaterialHeader(context);
        int[] colors = {0xff0000, 0x00ff00, 0x0000ff};
        header.setColorSchemeColors(colors);
        header.setLayoutParams(new PtrFrameLayout.LayoutParams(-1, -2));
        header.setPtrFrameLayout(mRefreshFrame);

        mRefreshFrame.setLoadingMinTime(1000);
        mRefreshFrame.setHeaderView(header);
        mRefreshFrame.addPtrUIHandler(header);
        mRefreshFrame.setResistance(1.7f);
        mRefreshFrame.setRatioOfHeaderHeightToRefresh(1.2f);
        mRefreshFrame.setDurationToClose(200);
        mRefreshFrame.setDurationToCloseHeader(700);
        mRefreshFrame.setPullToRefresh(false);
        mRefreshFrame.setKeepHeaderWhenRefresh(true);
        mScrollListener = new EndlessRecyclerOnScrollListener(mEventCollectionLayoutManager) {
            @Override
            public void onLoadMore(int currentPage) {
                mLoadingType = LOADING_TYPES.REQUEST_NEW_EVENT_ITEMS;
                mRefreshFrame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshFrame.autoRefresh(true);
                    }
                }, 100);
            }
        };

        mEventCollectionList.addOnItemTouchListener(
                new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        mLoadingType = LOADING_TYPES.PULL_TO_REFRESH;
                        mEventItemSelectListener.onEventItemSelect(mEventModel.getEventItemsList().get(position));
                    }
                })
        );

        mEventCollectionList.setOnScrollListener(mScrollListener);
        mEventCollectionAdapter.setEventItems(mEventModel.getEventItemsList());
        return view;
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
        if (mLoadingType == LOADING_TYPES.PULL_TO_REFRESH) {
            mScrollListener.reset();
        }
        mLoadingType = LOADING_TYPES.PULL_TO_REFRESH;
        mEventCollectionAdapter.setEventItems(mEventModel.getEventItemsList());
        mRefreshFrame.refreshComplete();
    }

    @Override
    public void onLoaderReset(Loader<Void> loader) {
        if (mLoadingType == LOADING_TYPES.PULL_TO_REFRESH) {
            mScrollListener.reset();
        }
        mLoadingType = LOADING_TYPES.PULL_TO_REFRESH;
        mEventCollectionAdapter.setEventItems(null);
        mRefreshFrame.refreshComplete();
    }
}
