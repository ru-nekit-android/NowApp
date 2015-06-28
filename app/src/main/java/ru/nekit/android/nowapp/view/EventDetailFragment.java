package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.SearchView;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventApiExecutor;
import ru.nekit.android.nowapp.model.EventApiCallResult;
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.model.vo.EventAdvert;
import ru.nekit.android.nowapp.model.vo.EventStats;
import ru.nekit.android.nowapp.model.EventsModel;
import ru.nekit.android.nowapp.model.EventToCalendarLoader;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;
import ru.nekit.android.nowapp.modelView.listeners.IEventPosterSelectListener;
import ru.nekit.android.nowapp.modelView.listeners.IEventSelectListener;
import ru.nekit.android.nowapp.utils.RobotoTextAppearanceSpan;
import ru.nekit.android.nowapp.utils.TextViewUtils;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.OFFLINE;
import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

@SuppressWarnings("ResourceType")
public class EventDetailFragment extends Fragment implements View.OnClickListener, LoaderManager.LoaderCallbacks {

    public static final String TAG = "ru.nekit.android.event_detail_fragment";

    private static final int SHOW_ADVERT_DELAY = 3000;
    private static final int CALENDAR_LOADER_ID = 0;
    private static final int API_EXECUTOR_GROUP_ID = 1;
    private static final String KEY_EVENT_ITEM = "ru.nekit.android.event_item";
    private static final String KEY_SELECTED = "ru.nekit.android.selected";
    private static final int MAX_ZOOM = 19;
    private final FloatingActionButtonBehavior mFloatingActionButtonBehavior;
    private final Handler mHandler;
    private MapView mMapView;
    private final MapListener mMapListener = new MapListener() {
        @Override
        public boolean onScroll(ScrollEvent event) {
            return false;
        }

        @Override
        public boolean onZoom(ZoomEvent event) {
            checkZoomButtons();
            return false;
        }
    };
    private Event mEvent;
    private EventAdvert mEventAdvert;
    private IEventPosterSelectListener mEventPosterSelectListener;
    private IEventSelectListener mEventItemSelectListener;
    private ProgressWheel mProgressWheel;
    private GeoPoint mEventLocationPoint;
    private View mMapViewContainer;
    private ImageView mPosterThumbView;
    private boolean mPosterViewIsEmpty;
    private BroadcastReceiver mChangeApplicationStateReceiver;
    private MyLocationNewOverlay myLocationOverLay;
    private LocationManager mLocationManager;
    private ScrollView mScrollView;
    private TextView mDescriptionView;
    private TextView mViewsView;
    private TextView mLikesView;
    private ImageView mViewsIcon;
    private ImageView mLikesIcon;
    private Button mPhoneButton;
    private Button mSiteButton;
    private FloatingActionButton mFloatingActionButton;
    private LayoutInflater mInflater;
    private CoordinatorLayout mRootLayout;
    private EventToCalendarLink mEventToCalendarLink;
    private View mLikeContainer;
    private View mAdvertBlock;
    private Timer mTimer;
    private ProgressDialog mProgressDialog;

    public EventDetailFragment() {
        mFloatingActionButtonBehavior = new FloatingActionButtonBehavior();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static EventDetailFragment getInstance() {
        return new EventDetailFragment();
    }

    private float LOCATION_MIN_UPDATE_DISTANCE() {
        return getActivity().getResources().getInteger(R.integer.location_min_update_distance);
    }

    private long LOCATION_MIN_UPDATE_TIME() {
        return getActivity().getResources().getInteger(R.integer.location_min_update_time);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mChangeApplicationStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                applyApplicationState();
            }
        };
        mLocationManager = (LocationManager) getActivity().getSystemService(Activity.LOCATION_SERVICE);
    }

    private void applyApplicationState() {
        mMapView.setUseDataConnection(NowApplication.getState() == ONLINE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        createMap();
    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle arg = getArguments();
        FragmentActivity activity = getActivity();
        ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        applyApplicationState();
        NowApplication.registerForAppChangeStateNotification(mChangeApplicationStateReceiver);

        GpsMyLocationProvider gpsLocationProvider = new GpsMyLocationProvider(activity);
        gpsLocationProvider.setLocationUpdateMinTime(LOCATION_MIN_UPDATE_TIME());
        gpsLocationProvider.setLocationUpdateMinDistance(LOCATION_MIN_UPDATE_DISTANCE());
        myLocationOverLay.enableMyLocation(gpsLocationProvider);
        myLocationOverLay.setDrawAccuracyEnabled(true);

        initEventToCalendarLoader(EventToCalendarLoader.CHECK);

        boolean eventSelected = arg.getBoolean(KEY_SELECTED, false);
        if (eventSelected) {
            if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
                initEventApiExecutor(EventApiExecutor.METHOD_UPDATE_VIEWS, mEvent.id);
            } else {
                initEventApiExecutor(EventApiExecutor.METHOD_OBTAIN_STATS, mEvent.id);
            }
        } else {
            initEventApiExecutor(EventApiExecutor.METHOD_OBTAIN_STATS, mEvent.id);
        }
        arg.putBoolean(KEY_SELECTED, false);

        mFloatingActionButtonBehavior.activate();
        mFloatingActionButtonBehavior.validatePosition();

        updateEventStats(!eventSelected);

        mTimer = new Timer();

        mTimer.schedule(
                new TimerTask() {

                    @Override
                    public void run() {
                        mHandler.post(new Runnable() {
                            public void run() {
                                showAdvertBlock();
                                mTimer.cancel();
                            }
                        });
                    }
                },
                SHOW_ADVERT_DELAY);
    }

    private void showAdvertBlock() {
        if (isResumed()) {
            mEventAdvert = EventsModel.getInstance().getActualAdvert();
            if (mEventAdvert != null && NowApplication.getState() == OFFLINE) {
                if (EventsModel.getInstance().getEventById(mEventAdvert.eventId) == null) {
                    mEventAdvert = null;
                }
            }
            if (mEventAdvert != null) {
                expand(mAdvertBlock);
                ImageView advertIcon = (ImageView) mAdvertBlock.findViewById(R.id.advert_icon_view);
                Glide.with(this).load(mEventAdvert.logoThumb).into(advertIcon);
                TextView advertTextView = (TextView) mAdvertBlock.findViewById(R.id.advert_text_view);
                String advertString = getActivity().getResources().getString(R.string.attention_short) + " " + EventsModel.getStartTimeAlias(getActivity(), mEventAdvert) + " " + mEventAdvert.placeName + " " + mEventAdvert.name;
                advertTextView.setText(advertString);
                mAdvertBlock.setOnClickListener(this);
            }
        }
    }

    private void expand(final View view) {
        view.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = view.getMeasuredHeight();
        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                view.getLayoutParams().height = interpolatedTime == 1
                        ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration((int) (targetHeight / view.getContext().getResources().getDisplayMetrics().density) * 4);
        view.startAnimation(animation);
    }

    private void initEventApiExecutor(int method, int eventId) {
        Bundle args = new Bundle();
        args.putInt(EventApiExecutor.KEY_METHOD, method);
        args.putInt(EventApiExecutor.KEY_EVENT_ITEM_ID, eventId);
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        int id = API_EXECUTOR_GROUP_ID + method;
        final Loader<Integer> loader = loaderManager.getLoader(id);
        if (loader != null) {
            loaderManager.restartLoader(id, args, this);
        } else {
            loaderManager.initLoader(id, args, this);
        }
    }

    private void initEventToCalendarLoader(int method) {
        Bundle args = new Bundle();
        args.putInt(EventToCalendarLoader.KEY_METHOD, method);
        args.putInt(EventToCalendarLoader.KEY_EVENT_ITEM_ID, mEvent.id);
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        int id = CALENDAR_LOADER_ID;
        final Loader<EventToCalendarLink> loader = loaderManager.getLoader(id);
        if (loader != null) {
            loaderManager.restartLoader(id, args, this);
        } else {
            loaderManager.initLoader(id, args, this);
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        Loader loader = null;
        switch (id) {
            case CALENDAR_LOADER_ID:

                mEventToCalendarLink = null;
                loader = new EventToCalendarLoader(getActivity(), args);

                break;

            case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_OBTAIN_STATS:
            case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_UPDATE_VIEWS:
            case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_LIKE:
            case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_OBTAIN_EVENT:

                loader = new EventApiExecutor(getActivity(), args);
                if (id == API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_OBTAIN_EVENT) {
                    mProgressDialog = showProgressDialog();
                }

                break;

            default:

        }
        if (loader != null) {
            loader.forceLoad();
        }
        return loader;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean calendarLinkIsPresent = mEventToCalendarLink != null;
        setMenuItVisible(R.id.action_remove_from_calendar, calendarLinkIsPresent, menu);
        setMenuItVisible(R.id.action_show_in_calendar, calendarLinkIsPresent, menu);
        setMenuItVisible(R.id.action_add_to_calendar, !calendarLinkIsPresent, menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onLoadFinished(Loader loader, Object result) {
        if (isResumed()) {
            switch (loader.getId()) {
                case CALENDAR_LOADER_ID:

                    Pair<Integer, EventToCalendarLink> calendarResult = (Pair<Integer, EventToCalendarLink>) result;
                    mEventToCalendarLink = calendarResult.second;
                    int messageId = 0;
                    if (calendarResult.first == EventToCalendarLoader.ADD) {
                        messageId = R.string.add_to_calendar_message;
                    } else if (calendarResult.first == EventToCalendarLoader.REMOVE) {
                        messageId = R.string.remove_from_calendar_message;
                    }
                    if (messageId != 0) {
                        if (mFloatingActionButtonBehavior.doBottomViewOverlap()) {
                            mFloatingActionButtonBehavior.allowMoveOnScroll(false);
                            mFloatingActionButtonBehavior.allowMoveOnSnackbarShow(true);
                        } else {
                            mFloatingActionButtonBehavior.allowMoveOnSnackbarShow(false);
                        }
                        Snackbar snackbar = Snackbar.make(mFloatingActionButton, messageId, Snackbar.LENGTH_LONG);
                        snackbar.setActionTextColor(EventsModel.getCategoryColor(mEvent.category));
                        if (calendarResult.first == EventToCalendarLoader.ADD) {
                            snackbar.setAction(R.string.open_calendar_message, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    openCalendarApplication();
                                }
                            });
                        }
                        snackbar.show();
                        mFloatingActionButton.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mFloatingActionButtonBehavior.allowMoveOnScroll(true);
                                mFloatingActionButtonBehavior.validatePosition();
                            }
                        }, 3250);
                    }

                    break;

                case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_OBTAIN_STATS:

                    updateEventStats(true);

                    break;

                case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_LIKE:
                case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_UPDATE_VIEWS:

                    initEventApiExecutor(EventApiExecutor.METHOD_OBTAIN_STATS, mEvent.id);

                    break;

                case API_EXECUTOR_GROUP_ID + EventApiExecutor.METHOD_OBTAIN_EVENT:

                    final Event event = ((EventApiCallResult) result).event;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mEventItemSelectListener.onEventSelect(event);
                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                            }
                        }
                    });

                    break;

                default:
                    break;

            }
        }
    }

    private void updateEventStats(boolean confirmed) {
        Resources resources = getActivity().getResources();
        EventStats eventStats = mEvent.stats;
        if (eventStats == null) {
            eventStats = EventsModel.getInstance().obtainEventStatsByEventId(mEvent.id);
        }
        boolean myLike = eventStats.myLikeStatus != 0;
        mViewsView.setText(eventStats.getViews(NowApplication.getState() == NowApplication.APP_STATE.ONLINE, confirmed));
        mLikesView.setText(eventStats.getLikes());
        mFloatingActionButton.setEnabled(!myLike);
        int normalColor = resources.getColor(R.color.event_stats_normal);
        int activeColor = resources.getColor(R.color.event_stats_active);
        int likeColor = myLike ? activeColor : normalColor;
        mViewsIcon.getDrawable().setColorFilter(new LightingColorFilter(normalColor, normalColor));
        mViewsIcon.setImageDrawable(mViewsIcon.getDrawable());
        mLikesIcon.getDrawable().setColorFilter(new LightingColorFilter(likeColor, likeColor));
        mLikesIcon.setImageDrawable(mLikesIcon.getDrawable());
        mLikeContainer.setBackgroundResource(myLike ? R.drawable.event_stats_bacground : 0);
        mLikesView.setTextColor(likeColor);
        mFloatingActionButton.setImageDrawable(resources.getDrawable(myLike ? R.drawable.ic_favorite_white : R.drawable.ic_favorite_border));
    }

    private void setMenuItVisible(int id, boolean visible, Menu menu) {
        if (menu != null) {
            menu.findItem(id).setVisible(visible);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    private void openCalendarApplication() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, mEventToCalendarLink.getCalendarEventID()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        NowApplication.unregisterForAppChangeStateNotification(mChangeApplicationStateReceiver);
        mFloatingActionButtonBehavior.deactivate();
        myLocationOverLay.disableMyLocation();
        mTimer.cancel();
    }

    private void createMap() {

        Context context = getActivity();

        //configure map view container :: set height - half of screen height
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        mMapViewContainer.getLayoutParams().height = (height + getActivity().getResources().getDimensionPixelOffset(R.dimen.logo_max_height)) / 2;

        //configure map view :: set parameters + add marker
        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mEventLocationPoint = new GeoPoint(mEvent.lat, mEvent.lng);
        mMapView.getController().setZoom(MAX_ZOOM);
        mMapView.getController().setCenter(mEventLocationPoint);

        //configure map view overlay :: add map marker
        final ArrayList<OverlayItem> items = new ArrayList<>();
        OverlayItem overlayItem = new OverlayItem(null, null, mEventLocationPoint);
        overlayItem.setMarkerHotspot(OverlayItem.HotspotPlace.BOTTOM_CENTER);
        items.add(overlayItem);
        DefaultResourceProxyImpl resProxyImp = new DefaultResourceProxyImpl(context);
        ItemizedIconOverlay markersOverlay = new ItemizedIconOverlay<>(items, getResources().getDrawable(R.drawable.ic_action_location), null, resProxyImp);
        mMapView.getOverlays().add(markersOverlay);

        //configure map view overlay :: add my location marker
        myLocationOverLay = new MyLocationNewOverlay(context, mMapView);
        mMapView.getOverlays().add(myLocationOverLay);

        mMapView.setMapListener(mMapListener);

        checkZoomButtons();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SearchView searchView = (SearchView) getViewFromRoot(R.id.search_view);
        assert searchView != null;
        searchView.setVisibility(View.GONE);
    }

    private View getViewFromRoot(int id) {
        View view = getView();
        if (view != null) {
            return view.getRootView().findViewById(id);
        }
        return null;
    }

    private void checkZoomButtons() {
        View view = getView();
        if (view != null) {
            View button = view.findViewById(R.id.zoom_plus);
            boolean active = mMapView.canZoomIn();
            button.setEnabled(active);
            if (active) {
                button.getBackground().setColorFilter(null);
            } else {
                button.getBackground().setColorFilter(new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY));
            }
            button = view.findViewById(R.id.zoom_minus);
            active = mMapView.canZoomOut();
            button.setEnabled(active);
            if (active) {
                button.getBackground().setColorFilter(null);
            } else {
                button.getBackground().setColorFilter(new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arg = getArguments();
        mEvent = arg.getParcelable(KEY_EVENT_ITEM);
        mInflater = inflater;
        return constructInterface(inflater.inflate(R.layout.fragment_event_detail, container, false));
    }

    private View constructInterface(View view) {

        final Context context = getActivity();

        mRootLayout = (CoordinatorLayout) view.findViewById(R.id.root_layout);
        TextView titleView = (TextView) view.findViewById(R.id.title_view);
        ArrayList<String> eventNameArray = ru.nekit.android.nowapp.utils.StringUtil.wrapText(mEvent.name.toUpperCase());
        titleView.setLines(eventNameArray.size());
        titleView.setText(TextUtils.join("\n", eventNameArray));
        mPosterThumbView = (ImageView) view.findViewById(R.id.poster_thumb_view);
        mProgressWheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);
        mProgressWheel.setVisibility(View.VISIBLE);
        if (mEvent.posterThumb != null && !"".equals(mEvent.posterThumb)) {
            Glide.with(context).load(mEvent.posterThumb).listener(new RequestListener<String, GlideDrawable>() {
                @Override
                public boolean onException(Exception exp, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                    mProgressWheel.setVisibility(View.GONE);
                    mPosterThumbView.setImageResource(R.drawable.event_poster_stub);
                    mPosterViewIsEmpty = true;
                    return true;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    mProgressWheel.setVisibility(View.GONE);
                    mPosterViewIsEmpty = false;
                    return false;
                }
            }).into(mPosterThumbView);
        } else {
            mProgressWheel.setVisibility(View.GONE);
            mPosterThumbView.setImageDrawable(context.getResources().getDrawable(R.drawable.event_poster_stub));
        }
        String logoThumb = mEvent.logoThumb;
        final ImageView logoView = (ImageView) view.findViewById(R.id.logo_view);
        TextView placeNameView = (TextView) view.findViewById(R.id.place_name_view);

        if ("".equals(logoThumb)) {
            logoView.setImageResource(R.drawable.ic_action_location_2);
        } else {
            logoView.setVisibility(View.VISIBLE);
            Glide.with(context).load(logoThumb).dontTransform().listener(new RequestListener<String, GlideDrawable>() {
                @Override
                public boolean onException(Exception exp, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                    Glide.with(context).load(R.drawable.ic_action_location_2).dontTransform().into(logoView);
                    return true;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    return false;
                }
            }).into(logoView);
        }
        int categoryDrawableId = EventsModel.getCategoryBigDrawable(mEvent.category);
        if (categoryDrawableId != 0) {
            ((ImageView) view.findViewById(R.id.category_type_view)).setImageDrawable(context.getResources().getDrawable(categoryDrawableId));
        }

        mPosterThumbView.setOnClickListener(this);

        TextView dayDateView = (TextView) view.findViewById(R.id.day_date_view);
        TextView monthView = (TextView) view.findViewById(R.id.month_view);
        TextView dayOfWeekView = (TextView) view.findViewById(R.id.day_of_week_view);
        TextView timeView = (TextView) view.findViewById(R.id.time_view);
        TextView entranceView = (TextView) view.findViewById(R.id.entrance_view);
        mDescriptionView = (TextView) view.findViewById(R.id.description_view);
        mViewsView = (TextView) view.findViewById(R.id.event_views);
        mLikesView = (TextView) view.findViewById(R.id.event_likes);
        mPhoneButton = (Button) view.findViewById(R.id.phone_button);
        mSiteButton = (Button) view.findViewById(R.id.site_button);
        mPhoneButton.setVisibility(View.GONE);
        mSiteButton.setVisibility(View.GONE);
        if (!"".equals(mEvent.site)) {
            mSiteButton.setVisibility(View.VISIBLE);
            mSiteButton.setOnClickListener(this);
            mSiteButton.setText(mEvent.site);
            mSiteButton.setTransformationMethod(null);
        }
        if (!"".equals(mEvent.phone)) {
            if (mEvent.phone.length() > 1) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                    mPhoneButton.setEnabled(false);
                } else {
                    mPhoneButton.setEnabled(true);
                    mPhoneButton.setOnClickListener(this);
                }
                mPhoneButton.setVisibility(View.VISIBLE);
                mPhoneButton.setText(mEvent.phone);
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(mEvent.date));
        dayDateView.setText(String.format("%d", calendar.get(Calendar.DAY_OF_MONTH)));
        monthView.setText(new DateFormatSymbols().getShortMonths()[calendar.get(Calendar.MONTH)].toLowerCase());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        dayOfWeekView.setText(getResources().getTextArray(R.array.day_of_week)[dayOfWeek]);
        calendar.set(Calendar.SECOND, (int) EventsModel.getEventStartTimeInSeconds(mEvent));
        createEventTimeTextBlock(context, timeView, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        createEventEntranceTextBlock(context, entranceView, mEvent.entrance);
        mDescriptionView.setText(mEvent.eventDescription);
        mMapView = (MapView) view.findViewById(R.id.map_view);
        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        View mapScrollFakeView = view.findViewById(R.id.map_scroll_fake_view);

        mapScrollFakeView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mScrollView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    case MotionEvent.ACTION_UP:
                        mScrollView.requestDisallowInterceptTouchEvent(false);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        mScrollView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    default:
                        return true;
                }
            }
        });

        view.findViewById(R.id.zoom_minus).setOnClickListener(this);
        view.findViewById(R.id.zoom_plus).setOnClickListener(this);
        view.findViewById(R.id.place_group).setOnClickListener(this);
        view.findViewById(R.id.my_location).setOnClickListener(this);

        createEventPlaceTextBlock(context, placeNameView, mEvent.placeName, mEvent.address);

        mMapViewContainer = view.findViewById(R.id.map_view_container);

        //TODO: do not work
        mScrollView.scrollTo(0, 0);

        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_event);
        mFloatingActionButton.setBackgroundTintList(ColorStateList.valueOf(EventsModel.getCategoryColor(mEvent.category)));
        CoordinatorLayout.LayoutParams fabLayoutParams = (CoordinatorLayout.LayoutParams) mFloatingActionButton.getLayoutParams();
        fabLayoutParams.setBehavior(mFloatingActionButtonBehavior);
        mFloatingActionButton.setOnClickListener(this);

        mViewsIcon = (ImageView) view.findViewById(R.id.event_view_icon);
        mLikesIcon = (ImageView) view.findViewById(R.id.event_like_icon);
        mLikeContainer = view.findViewById(R.id.event_like_container);

        mAdvertBlock = view.findViewById(R.id.advert_block);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
        setMenuItVisible(R.id.action_add_to_calendar, false, menu);
        setMenuItVisible(R.id.action_remove_from_calendar, false, menu);
        setMenuItVisible(R.id.action_show_in_calendar, false, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Context context = getActivity();
        switch (item.getItemId()) {

            case R.id.action_share:

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mEvent.name);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, mEvent.eventDescription);
                startActivity(Intent.createChooser(sharingIntent, context.getResources().getString(R.string.share_title)));

                return true;

            case R.id.action_add_to_calendar:

                initEventToCalendarLoader(EventToCalendarLoader.ADD);

                return true;

            case R.id.action_remove_from_calendar:

                initEventToCalendarLoader(EventToCalendarLoader.REMOVE);

                return true;

            case R.id.action_show_in_calendar:

                openCalendarApplication();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createEventPlaceTextBlock(Context context, TextView placeNameView, String placeName, String address) {
        SpannableString placeNameSpan = new SpannableString(placeName);
        placeNameSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.Place_Name), 0, placeName.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString addressSpan = new SpannableString(address);
        addressSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.Place_Address), 0, address.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        placeNameView.setText(address.equals(placeName) ? placeNameSpan : TextUtils.concat(placeNameSpan, "\n", addressSpan));
    }

    private void createEventEntranceTextBlock(Context context, TextView entranceView, String entrance) {
        String title = context.getResources().getString(R.string.entrance_title);
        SpannableString titleSpan = new SpannableString(title);
        titleSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.EntranceTitle), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString entranceSpan = new SpannableString(entrance);
        entranceSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.Entrance), 0, entrance.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        entranceView.setText(TextUtils.concat(titleSpan, "\n", entranceSpan));
    }

    private void createEventTimeTextBlock(Context context, TextView timeView, int hour, int minute) {
        String hourTextValue = String.format("%d", hour);
        String minuteTextValue = String.format("%d", minute);
        if (hour < 9) {
            hourTextValue = "0".concat(hourTextValue);
        }
        if (minute < 9) {
            minuteTextValue = "0".concat(minuteTextValue);
        }
        TypedArray attrArray = context.obtainStyledAttributes(R.style.HourText, R.styleable.StyleAttributes);
        int hourTextSize = 0;
        try {
            hourTextSize = attrArray.getDimensionPixelOffset(0, -1);
        } catch (RuntimeException exp) {
            //empty
        } finally {
            attrArray.recycle();
        }

        SpannableString hourValueSpan = new SpannableString(hourTextValue);
        hourValueSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.HourText), 0, hourTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableStringBuilder minuteSpanBuilder = new SpannableStringBuilder(minuteTextValue);
        minuteSpanBuilder.setSpan(new RobotoTextAppearanceSpan(context, R.style.MinuteText), 0, minuteTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        minuteSpanBuilder.setSpan(TextViewUtils.getSuperscriptSpanAdjuster(context, hourTextValue, hourTextSize), 0, minuteTextValue.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence finalText = TextUtils.concat(hourValueSpan, minuteSpanBuilder);
        timeView.setText(finalText);
    }

    public void setEventItem(Event event) {
        Bundle arg = getArguments();
        if (arg == null) {
            arg = new Bundle();
        }
        arg.putParcelable(KEY_EVENT_ITEM, event);
        arg.putBoolean(KEY_SELECTED, true);
        setArguments(arg);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEventPosterSelectListener = (IEventPosterSelectListener) getActivity();
        } catch (ClassCastException exp) {
            throw new ClassCastException(activity.toString()
                    + " must implement IEventPosterSelectListener");
        }
        try {
            mEventItemSelectListener = (IEventSelectListener) getActivity();
        } catch (ClassCastException exp) {
            throw new ClassCastException(activity.toString()
                    + " must implement IEventSelectListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEventPosterSelectListener = null;
        mEventItemSelectListener = null;
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.phone_button:

                intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + mEvent.phone));
                startActivity(intent);

                break;

            case R.id.site_button:

                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(mEvent.site));
                startActivity(intent);

                break;

            case R.id.place_group:

                mMapView.getController().animateTo(mEventLocationPoint);
                checkZoomButtons();

                break;

            case R.id.zoom_minus:

                mMapView.getController().zoomOut();
                checkZoomButtons();

                break;

            case R.id.zoom_plus:

                mMapView.getController().zoomIn();
                checkZoomButtons();

                break;

            case R.id.poster_thumb_view:

                if (!mPosterViewIsEmpty) {
                    mEventPosterSelectListener.onEventItemPosterSelect(mEvent.posterOriginal);
                }

                break;

            case R.id.my_location:

                IGeoPoint myLocationPoint = myLocationOverLay.getMyLocation();
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showGPSDisabledDialog();
                }
                if (myLocationPoint != null) {
                    mMapView.getController().animateTo(myLocationPoint);
                } else {
                    //wait??
                }
                checkZoomButtons();

                break;

            case R.id.fab_event:

                EventStats eventStats = mEvent.stats;
                if (eventStats.myLikeStatus == 0) {
                    showAddToCalendarDialog();
                    initEventApiExecutor(EventApiExecutor.METHOD_LIKE, mEvent.id);
                }

                break;

            case R.id.advert_block:

                initEventApiExecutor(EventApiExecutor.METHOD_OBTAIN_EVENT, mEventAdvert.eventId);

                break;

            default:
                break;
        }
    }

    private void showAddToCalendarDialog() {
        Context context = getActivity();
        AlertDialog.Builder builder;
        AppCompatDialog dialog;
        View dialogContentView = mInflater.inflate(R.layout.dialog_content, null, false);
        View dialogTitleView = mInflater.inflate(R.layout.dialog_title, null, false);
        TextView dialogTextView = (TextView) dialogContentView.findViewById(R.id.text_view);
        dialogTextView.setTextAppearance(context, R.style.DialogContent);
        TextView dialogTitleTextView = (TextView) dialogTitleView.findViewById(R.id.text_view);
        dialogTitleTextView.setTextAppearance(context, R.style.DialogTitle);
        builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        builder.setCancelable(true)
                .setView(dialogContentView)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        initEventToCalendarLoader(EventToCalendarLoader.ADD);
                    }
                }).setNegativeButton(R.string.no, null)
                .setCustomTitle(dialogTitleView);
        builder.setInverseBackgroundForced(true);
        dialogTitleTextView.setText(R.string.add_to_calendar_dialog_title);
        dialogTextView.setText(R.string.add_to_calendar_dialog_message);
        dialog = builder.create();
        dialog.show();
    }

    private void showGPSDisabledDialog() {
        Context context = getActivity();
        AlertDialog.Builder builder;
        AppCompatDialog dialog;
        View dialogContentView = mInflater.inflate(R.layout.dialog_content, null, false);
        View dialogTitleView = mInflater.inflate(R.layout.dialog_title, null, false);
        TextView dialogTextView = (TextView) dialogContentView.findViewById(R.id.text_view);
        dialogTextView.setTextAppearance(context, R.style.DialogContent);
        TextView dialogTitleTextView = (TextView) dialogTitleView.findViewById(R.id.text_view);
        dialogTitleTextView.setTextAppearance(context, R.style.DialogTitle);
        builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        builder.setCancelable(true)
                .setView(dialogContentView)
                .setPositiveButton(R.string.switch_on, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressWheel.setVisibility(View.VISIBLE);
                        Intent callGPSSettingIntent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                }).setNegativeButton(R.string.no, null)
                .setCustomTitle(dialogTitleView);
        builder.setInverseBackgroundForced(true);
        dialogTitleTextView.setText(R.string.gps_is_off);
        dialogTextView.setText(R.string.gps_switch_on_ask);
        dialog = builder.create();
        dialog.show();
    }

    private ProgressDialog showProgressDialog() {
        Context context = getActivity();
        ProgressDialog progress = new ProgressDialog(context, ProgressDialog.THEME_HOLO_LIGHT);
        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();
        progress.setMessage(context.getResources().getString(R.string.loading));
        return progress;
    }

    class FloatingActionButtonBehavior extends FloatingActionButton.Behavior {

        private float mTranslationY;
        private boolean mAllowMoveOnScroll, mAllowMoveOnSnackbarShow;

        private final ViewTreeObserver.OnGlobalLayoutListener floatingActionButtonLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                validatePosition();
            }

        };

        private ViewTreeObserver.OnScrollChangedListener scrollListener = new ViewTreeObserver.OnScrollChangedListener() {

            @Override
            public void onScrollChanged() {
                validatePosition();
            }
        };

        public FloatingActionButtonBehavior() {
            mAllowMoveOnScroll = true;
            mAllowMoveOnSnackbarShow = true;
        }

        public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
            updateFabTranslationForSnackbar(parent, child, dependency);
            return false;
        }

        private void updateFabTranslationForSnackbar(CoordinatorLayout parent, FloatingActionButton fab, View snackbar) {
            if (mAllowMoveOnSnackbarShow) {
                float translationY = this.getFabTranslationYForSnackbar(parent, fab);
                if (translationY != this.mTranslationY) {
                    ViewCompat.animate(fab).cancel();
                    if (Math.abs(translationY - this.mTranslationY) == (float) snackbar.getHeight()) {
                        ViewCompat.animate(fab).translationY(translationY).setInterpolator(new FastOutSlowInInterpolator()).setListener(null);
                    } else {
                        if (doBottomViewOverlap(false)) {
                            ViewCompat.setTranslationY(fab, translationY);
                        } else if (getAppHeight() - getBottomViewBottom() + translationY < 0) {
                            ViewCompat.setTranslationY(fab, translationY);
                        }
                    }
                    this.mTranslationY = translationY;
                }
            }
        }

        private float getFabTranslationYForSnackbar(CoordinatorLayout parent, FloatingActionButton fab) {
            float minOffset = 0.0F;
            List dependencies = parent.getDependencies(fab);
            int i = 0;

            for (int z = dependencies.size(); i < z; ++i) {
                View view = (View) dependencies.get(i);
                if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                    minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - (float) view.getHeight());
                }
            }
            return minOffset;
        }

        public void activate() {
            mFloatingActionButton.getViewTreeObserver().addOnGlobalLayoutListener(floatingActionButtonLayoutListener);
            mScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        }

        public void deactivate() {
            mFloatingActionButton.getViewTreeObserver().removeGlobalOnLayoutListener(floatingActionButtonLayoutListener);
            mScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        }

        private void validatePosition() {
            if (EventDetailFragment.this.isResumed()) {
                float appWorkAreaHeight = mRootLayout.getHeight();
                float appHeight = getAppHeight();
                int space = getActivity().getResources().getDimensionPixelOffset(R.dimen.fab_compat_margin);
                int fabHeight = mFloatingActionButton.getHeight();
                float bottomViewBottom = getBottomViewBottom();
                if (mAllowMoveOnScroll) {
                    if (appHeight < bottomViewBottom) {
                        mFloatingActionButton.setY(appWorkAreaHeight - fabHeight - space);
                    } else {
                        mFloatingActionButton.setY(appWorkAreaHeight - (appHeight - bottomViewBottom) - fabHeight - space);
                    }
                }
                mDescriptionView.setPadding(0, 0, 0, (fabHeight + space) / 2);
            }
        }

        private float getAppHeight() {
            float appWorkAreaHeight = mRootLayout.getHeight();
            int[] appWorkAreaCords = new int[2];
            mRootLayout.getLocationOnScreen(appWorkAreaCords);
            return appWorkAreaCords[1] + appWorkAreaHeight;
        }

        private float getBottomViewBottom() {
            View bottomView = mMapViewContainer;
            if (mPhoneButton.getVisibility() == View.VISIBLE) {
                bottomView = mPhoneButton;
            } else if (mSiteButton.getVisibility() == View.VISIBLE) {
                bottomView = mSiteButton;
            }
            int[] bottomViewCords = new int[2];
            bottomView.getLocationOnScreen(bottomViewCords);
            return bottomViewCords[1];
        }

        public boolean doBottomViewOverlap() {
            return doBottomViewOverlap(true);
        }

        private boolean doBottomViewOverlap(boolean withSnackBar) {
            return getAppHeight() < getBottomViewBottom() + (withSnackBar ? 1 : 0) * getActivity().getResources().getDimensionPixelOffset(R.dimen.snack_bar_height);
        }

        public void allowMoveOnScroll(boolean value) {
            mAllowMoveOnScroll = value;
        }

        public void allowMoveOnSnackbarShow(boolean value) {
            mAllowMoveOnSnackbarShow = value;
        }
    }
}
