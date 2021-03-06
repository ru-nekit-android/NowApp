package ru.nekit.android.nowapp.fragments;

import android.animation.Animator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.badoo.mobile.util.WeakHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.loaders.EventLoader;
import ru.nekit.android.nowapp.model.loaders.EventItemsLoader;
import ru.nekit.android.nowapp.model.EventsModel;
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.views.adapters.EventCollectionAdapter;
import ru.nekit.android.nowapp.listeners.IBackPressedListener;
import ru.nekit.android.nowapp.listeners.IEventClickListener;
import ru.nekit.android.nowapp.views.widgets.FloatingActionButtonAnimator;
import ru.nekit.android.nowapp.views.widgets.ScrollingGridLayoutManager;
import ru.nekit.android.nowapp.views.widgets.SoftKeyboardListenerLayout;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

public class EventCollectionFragment extends Fragment implements LoaderManager.LoaderCallbacks, IEventClickListener, View.OnClickListener, SearchView.OnQueryTextListener, IBackPressedListener, TextView.OnEditorActionListener, SoftKeyboardListenerLayout.OnSoftKeyboardListener, EventCollectionAdapter.OnLoadMorelListener {

    public static final String TAG = "ru.nekit.android.event_collection_fragment";

    private static final int LOADER_ID = 2, SEARCHER_ID = 3;
    private EventsModel mEventModel;
    @Nullable
    private String mQueryWithResult, mQuery;
    private boolean mSearchResultIsPresent, mKeyboardVisible;
    private int mCurrentPage;
    private LOADING_STATE mLoadingState;
    private MODE mMode;
    @Nullable
    private String mLoadingType;
    @Nullable
    private Event mWaitingForOpenItem;
    private EventCollectionAdapter mEventCollectionAdapter;
    private IEventClickListener mEventItemSelectListener;
    private FloatingActionButtonAnimator mFloatingActionButtonAnimator;
    private RecyclerView mEventItemsView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BroadcastReceiver mLocalBroadcastReceiver;
    private SearchView mSearchView;
    private TextView mSearchStatus;
    private EditText mSearchViewEditText;
    @Nullable
    private Animator.AnimatorListener mAnimatorListener;
    private FloatingActionButton mFloatingActionButton;
    private SoftKeyboardListenerLayout mScrollView;
    private final WeakHandler mHandler;

    public EventCollectionFragment() {
        mLoadingState = LOADING_STATE.LOADED;
        mLoadingType = null;
        mMode = MODE.NORMAL;
        mHandler = new WeakHandler();
    }

    private int smoothScrollDuration() {
        return getResources().getInteger(R.integer.smooth_scroll_duration);
    }

    private boolean featureLiveSearch() {
        return getResources().getBoolean(R.bool.feature_live_search);
    }

    @Override
    public void onEventClick(final Event event, boolean openNew) {
        if (mLoadingState == LOADING_STATE.LOADED) {
            if (searchViewVisible()) {
                applyMode(MODE.NORMAL);
                if (mKeyboardVisible) {
                    mWaitingForOpenItem = event;
                } else {
                    mFloatingActionButtonAnimator.hide();
                    mEventItemsView.requestFocus();
                    mEventItemSelectListener.onEventClick(event, false);
                }
            } else {
                mFloatingActionButtonAnimator.hide();
                mEventItemSelectListener.onEventClick(event, false);
            }
        }
    }

    private boolean searchQueryIsValid() {
        return TextUtils.getTrimmedLength(mSearchView.getQuery().toString()) > 0;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_collection, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mEventModel = NowApplication.getInstance().getEventModel();
        mCurrentPage = mEventModel.getCurrentPage();
        mLocalBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                String action = intent.getAction();
                if (action.equals(NowApplication.CHANGE_APPLICATION_STATE_NOTIFICATION)) {
                    applyApplicationState();
                    if (NowApplication.getInstance().getState() == ONLINE) {
                        mLoadingType = EventsModel.REFRESH_EVENTS;
                        mEventItemsView.smoothScrollToPosition(0);
                        mHandler.postDelayed(() -> {
                            mSwipeRefreshLayout.setRefreshing(true);
                            performLoad();
                        }, smoothScrollDuration());
                    } else {
                        performLoad();
                    }
                } else if (action.equals(EventsModel.LOAD_IN_BACKGROUND_NOTIFICATION)) {
                    if (mMode == MODE.SEARCH && mSearchResultIsPresent) {
                        performSearch(mQueryWithResult);
                    }
                    if (mMode == MODE.NORMAL) {
                        mLoadingType = EventsModel.REQUEST_NEW_EVENTS;
                        performLoad();
                    }
                }
            }
        };
    }

    private void setEventsFromModel() {
        mEventCollectionAdapter.setItems(mEventModel.getEvents());
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mMode == MODE.SEARCH) {
            if (searchQueryIsValid()) {
                performSearch(query);
            } else {
                mSearchResultIsPresent = false;
                setEventsFromModel();
            }
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (mMode == MODE.SEARCH) {
            if (featureLiveSearch()) {
                if (searchQueryIsValid()) {
                    performSearch(query);
                } else {
                    mSearchResultIsPresent = false;
                    setSearchStatus(null);
                    setEventsFromModel();
                }
            } else {
                if (searchQueryIsValid()) {
                    return true;
                }
            }
        }
        return false;
    }

    private View getViewFromRoot(int id) {
        View view = getView();
        if (view != null) {
            return view.getRootView().findViewById(id);
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();
        ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mLoadingType = EventsModel.REFRESH_EVENTS;
            setLoadingState(LOADING_STATE.LOADING);
            performLoad();
        });

        mFloatingActionButton.setOnClickListener(this);
        mFloatingActionButtonAnimator = new FloatingActionButtonAnimator(activity, mFloatingActionButton, mEventItemsView);
        mScrollView.setOnSoftKeyboardListener(this);
        if (mSearchResultIsPresent) {
            mHandler.postDelayed(this::restoreMode, getResources().getInteger(R.integer.slide_animation_duration) / 3 * 2);
        } else {
            mEventItemsView.requestFocus();

            if (mEventModel.dataIsActual()) {
                if (mEventModel.getCurrentPage() != mCurrentPage) {
                    mCurrentPage = mEventModel.getCurrentPage();
                }
            } else {
                if (NowApplication.getInstance().getState() == ONLINE) {
                    mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));
                    mLoadingType = EventsModel.REFRESH_EVENTS;
                    setLoadingState(LOADING_STATE.LOADING);
                    performLoad();
                }
            }
        }

        mFloatingActionButtonAnimator.attachToRecyclerView();

        applyApplicationState();
        NowApplication.getInstance().registerForAppChangeStateNotification(mLocalBroadcastReceiver);
        mEventModel.registerForLoadInBackgroundResultNotification(mLocalBroadcastReceiver);

        setEventsFromModel();
    }

    private void restoreMode() {
        applyMode(MODE.SEARCH);
        mSearchView.setQuery(mQueryWithResult, true);
        mSearchViewEditText.requestFocus();
        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(mSearchViewEditText, 0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View nowTitle = getViewFromRoot(R.id.now_title);
        if (nowTitle != null) {
            nowTitle.setOnClickListener(this);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSearchView = (SearchView) getViewFromRoot(R.id.search_view);
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

            mSearchViewEditText = (EditText) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            mSearchViewEditText.setOnEditorActionListener(this);
            mSearchViewEditText.setHint(" " + getActivity().getString(R.string.search_hint));
            setCursorDrawableColor(mSearchViewEditText);

            ImageView searchCloseButton = (ImageView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
            searchCloseButton.setAlpha(192);
            searchCloseButton.setOnClickListener(this);
        }
    }

    public void setCursorDrawableColor(EditText editText) {
        try {
            final Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            fCursorDrawableRes.setAccessible(true);
            fCursorDrawableRes.set(editText, R.drawable.edit_text_cursor);
        } catch (final Throwable ignored) {
        }
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            if (!searchQueryIsValid()) {
                setEventsFromModel();
                return true;
            } else {
                onQueryTextSubmit(mSearchView.getQuery().toString());
            }
        }
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_collection, container, false);
        mEventItemsView = (RecyclerView) view.findViewById(R.id.event_collection_list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_events);
        mSearchStatus = (TextView) view.findViewById(R.id.search_status);
        mScrollView = (SoftKeyboardListenerLayout) view.findViewById(R.id.root_layout);

        int listColumn = getResources().getInteger(R.integer.event_collection_column_count);

        mEventCollectionAdapter = new EventCollectionAdapter(getActivity(), listColumn);
        mEventCollectionAdapter.setHasStableIds(true);
        mEventItemsView.setAdapter(mEventCollectionAdapter);

        mEventCollectionAdapter.registerRecyclerView(mEventItemsView);
        mEventCollectionAdapter.setLoadMoreListener(this);
        mEventCollectionAdapter.setOnItemClickListener(this);

        mEventItemsView.setHasFixedSize(true);
        mEventItemsView.setItemAnimator(new DefaultItemAnimator());

        ScrollingGridLayoutManager mEventCollectionLayoutManager = new ScrollingGridLayoutManager(getActivity(), listColumn, smoothScrollDuration());

        mEventCollectionLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mEventCollectionAdapter.getSpanSize(position);
            }
        });

        mEventItemsView.setLayoutManager(mEventCollectionLayoutManager);

        return view;
    }

    @Override
    public void onSoftKeyboardShown() {
        mKeyboardVisible = true;
    }

    @Override
    public void onSoftKeyboardHidden() {
        if (mWaitingForOpenItem != null) {
            mFloatingActionButtonAnimator.hide();
            mEventItemSelectListener.onEventClick(mWaitingForOpenItem, false);
            mWaitingForOpenItem = null;
        }
        mKeyboardVisible = false;
    }

    private void applyApplicationState() {
        updateRefreshLayoutEnabled();
    }

    private void setLoadingState(LOADING_STATE state) {
        boolean hideFAB = state == LOADING_STATE.LOADING && EventsModel.REFRESH_EVENTS.equals(mLoadingType);
        if (hideFAB) {
            mFloatingActionButtonAnimator.hide();
        } else {
            mFloatingActionButtonAnimator.show();
        }
        if (mLoadingState == state) return;
        mLoadingState = state;
        if (EventsModel.REQUEST_NEW_EVENTS.equals(mLoadingType)) {
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

    private void performSearch(String query) {
        mQuery = query;
        LoaderManager loaderManager = getLoaderManager();
        Bundle searchArgs = new Bundle();
        searchArgs.putString(EventItemsLoader.KEY_EVENT_ITEMS_SEARCH, query);
        final Loader loader = loaderManager.getLoader(SEARCHER_ID);
        if (loader != null) {
            loaderManager.restartLoader(SEARCHER_ID, searchArgs, EventCollectionFragment.this);
        } else {
            loaderManager.initLoader(SEARCHER_ID, searchArgs, EventCollectionFragment.this);
        }
    }

    private void performLoad() {
        LoaderManager loaderManager = getLoaderManager();
        Bundle loaderArgs = new Bundle();
        loaderArgs.putString(EventsModel.LOADING_TYPE, mLoadingType);
        final Loader loader = loaderManager.getLoader(LOADER_ID);
        if (loader != null) {
            loaderManager.restartLoader(LOADER_ID, loaderArgs, EventCollectionFragment.this);
        } else {
            loaderManager.initLoader(LOADER_ID, loaderArgs, EventCollectionFragment.this);
        }
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        try {
            mEventItemSelectListener = (IEventClickListener) getActivity();
        } catch (ClassCastException exp) {
            throw new ClassCastException(activity.toString()
                    + " must implement IEventClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEventItemSelectListener = null;
    }

    @Override
    public void onDestroy() {
        mEventCollectionAdapter.unregisterRecyclerView(mEventItemsView);
        mEventCollectionAdapter.setOnItemClickListener(null);
        mEventCollectionAdapter.setLoadMoreListener(null);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setOnRefreshListener(null);
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.destroyLoader(LOADER_ID);
        loaderManager.destroyLoader(SEARCHER_ID);
        setLoadingState(LOADING_STATE.LOADED);

        mFloatingActionButtonAnimator.dettachFromRecyclerView();
        mFloatingActionButton.setOnClickListener(null);
        mScrollView.setOnSoftKeyboardListener(null);

        NowApplication.getInstance().unregisterForAppChangeStateNotification(mLocalBroadcastReceiver);
        mEventModel.unregisterForLoadInBackgroundResultNotification(mLocalBroadcastReceiver);

        //NowApplication.getInstance().getWatcher().watch(this);

    }

    @Nullable
    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        Loader loader = null;
        switch (id) {
            case LOADER_ID:

                loader = new EventLoader(getActivity(), args);

                break;
            case SEARCHER_ID:

                loader = new EventItemsLoader(getActivity(), args);

                break;
            default:

        }
        if (loader != null) {
            loader.forceLoad();
        }
        return loader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader loader, Object result) {
        if (isResumed()) {
            switch (loader.getId()) {
                case LOADER_ID:

                    int resultForLoad = (int) result;
                    mLoadingType = EventsModel.REFRESH_EVENTS;
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (resultForLoad == EventsModel.RESULT_OK) {
                        setEventsFromModel();
                        mCurrentPage = mEventModel.getCurrentPage();
                    } else {
                        Toast.makeText(getActivity(), Html.fromHtml(getResources().getString(R.string.error_while_data_loading)), Toast.LENGTH_LONG).show();
                    }
                    setLoadingState(LOADING_STATE.LOADED);

                    break;

                case SEARCHER_ID:

                    ArrayList<Event> resultForSearch = (ArrayList<Event>) result;
                    mSearchResultIsPresent = resultForSearch.size() > 0;
                    mQueryWithResult = mSearchResultIsPresent ? mQuery : null;
                    mEventCollectionAdapter.setItems(resultForSearch);
                    boolean isEmpty = resultForSearch.size() == 0;
                    setSearchStatus(isEmpty ? getActivity().getString(R.string.nothing_found) : null);
                    if (isEmpty) {
                        mFloatingActionButtonAnimator.show();
                    }

                    break;
                default:

            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {
        switch (loader.getId()) {

            case LOADER_ID:

                setLoadingState(LOADING_STATE.LOADED);
                mLoadingType = EventsModel.REFRESH_EVENTS;
                mSwipeRefreshLayout.setRefreshing(false);

                break;

            case SEARCHER_ID:

                break;
            default:

        }
    }

    public void setSearchStatus(@Nullable String searchStatus) {
        mSearchStatus.setVisibility(searchStatus == null ? View.INVISIBLE : View.VISIBLE);
        if (searchStatus != null) {
            mSearchStatus.setText(searchStatus);
        }
    }

    private void applyMode(MODE mode) {
        mMode = mode;
        mSearchView.setIconified(false);

        final boolean searchVisible = mode == MODE.SEARCH;

        final View titleContainer = getViewFromRoot(R.id.title_container);

        mAnimatorListener = new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (titleContainer != null) {
                    titleContainer.setVisibility(searchVisible ? View.INVISIBLE : View.VISIBLE);
                }
                mSearchView.setVisibility(searchVisible ? View.VISIBLE : View.GONE);
                if (isResumed()) {
                    animateFade(true, searchVisible ? mSearchView : titleContainer);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };

        animateFade(false, searchVisible ? titleContainer : mSearchView);

        updateRefreshLayoutEnabled();
        if (!searchVisible) {
            setSearchStatus(null);
        }
    }

    private void animateFade(boolean in, @NonNull View view) {
        Resources res = getResources();
        if (res != null) {
            int duration = getResources().getInteger(R.integer.change_mode_animation_duration);
            view.animate().alpha(in ? 1 : 0).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(mAnimatorListener).setDuration(duration);
        }
    }

    private void updateRefreshLayoutEnabled() {
        mSwipeRefreshLayout.setEnabled(mMode == MODE.NORMAL && NowApplication.getInstance().getState() == ONLINE);
    }

    private boolean searchViewVisible() {
        return mSearchView.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onBackPressed() {
        if (searchViewVisible()) {
            mSearchView.setQuery("", true);
            applyMode(MODE.NORMAL);
            setEventsFromModel();
        }
    }

    @Override
    public void onClick(@NonNull View view) {

        switch (view.getId()) {
            case R.id.now_title:
                if (isResumed()) {
                    mEventItemsView.smoothScrollToPosition(0);
                }
                break;

            case R.id.fab_events:

                if (mKeyboardVisible) {
                    mEventItemsView.smoothScrollToPosition(0);
                    if (searchViewVisible()) {
                        mSearchView.setQuery("", true);
                        applyMode(MODE.NORMAL);
                    } else {
                        applyMode(MODE.SEARCH);
                    }
                } else {
                    applyMode(MODE.SEARCH);
                }

                break;

            case android.support.v7.appcompat.R.id.search_close_btn:

                if (searchQueryIsValid()) {
                    mSearchView.setQuery("", true);
                    mFloatingActionButtonAnimator.show();
                } else {
                    applyMode(MODE.NORMAL);
                    setEventsFromModel();
                }

                break;

            default:
                break;
        }

    }

    @Override
    public void onLoadMore() {
        if (mMode == MODE.NORMAL || !mSearchResultIsPresent) {
            mLoadingType = EventsModel.REQUEST_NEW_EVENTS;
            setLoadingState(LOADING_STATE.LOADING);
        }
    }

    enum MODE {
        NORMAL, SEARCH
    }

    enum LOADING_STATE {
        LOADING, LOADED
    }
}
