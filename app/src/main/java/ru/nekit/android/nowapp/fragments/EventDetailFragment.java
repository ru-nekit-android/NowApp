package ru.nekit.android.nowapp.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.badoo.mobile.util.WeakHandler;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.seismic.ShakeDetector;

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

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.listeners.IEventClickListener;
import ru.nekit.android.nowapp.listeners.IEventPosterSelectListener;
import ru.nekit.android.nowapp.listeners.IFlayerDialogListener;
import ru.nekit.android.nowapp.model.EventApiCallResult;
import ru.nekit.android.nowapp.model.EventsModel;
import ru.nekit.android.nowapp.model.loaders.EventApiLoader;
import ru.nekit.android.nowapp.model.loaders.EventToCalendarLoader;
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.model.vo.EventAdvert;
import ru.nekit.android.nowapp.model.vo.EventStats;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;
import ru.nekit.android.nowapp.utils.RobotoTextAppearanceSpan;
import ru.nekit.android.nowapp.utils.TextViewUtils;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

@SuppressWarnings("ResourceType")
public class EventDetailFragment extends Fragment implements View.OnClickListener, LoaderManager.LoaderCallbacks, ShakeDetector.Listener, IFlayerDialogListener {

    public static final String TAG = "ru.nekit.android.event_detail_fragment";

    private static final int SHOW_ADVERT_DELAY = 3000;
    private static final int CALENDAR_LOADER_ID = 0;
    private static final int API_EXECUTOR_GROUP_ID = 1;
    private static final String KEY_EVENT_ITEM = "ru.nekit.android.event_item";
    private static final String KEY_ADVERT_POSSIBILITY = "ru.nekit.android.possibility";
    private static final int MAX_ZOOM = 19;

    @NonNull
    private final FloatingActionButtonBehavior mFloatingActionButtonBehavior;
    @NonNull
    private final WeakHandler mHandler;
    @NonNull
    private final MapListener mMapListener;
    private EventsModel mEventModel;
    private Timer mTimer;
    private Event mEvent, mEventLinkToAdvert;
    @Nullable
    private EventAdvert mEventAdvert;
    private IEventPosterSelectListener mEventPosterSelectListener;
    private IEventClickListener mEventClickListener;
    private ProgressWheel mProgressWheel;
    private GeoPoint mEventLocationPoint;
    private MapView mMapView;
    private boolean mPosterViewIsEmpty;
    private BroadcastReceiver mChangeApplicationStateReceiver;
    private MyLocationNewOverlay myLocationOverLay;
    private LocationManager mLocationManager;
    private ScrollView mScrollView;
    private TextView mDescriptionView, mViewsView, mLikesView;
    private ImageView mViewsIcon, mLikesIcon, mPosterThumbView, mHandView;
    private Button mPhoneButton, mSiteButton;
    private FloatingActionButton mFloatingActionButton;
    private LayoutInflater mInflater;
    private CoordinatorLayout mRootLayout;
    private EventToCalendarLink mEventToCalendarLink;
    private View mMapViewContainer, mLikeContainer, mAdvertBlock;
    private long obtainAdvertCallTime;
    private ShakeDetector mShakeDetector;
    private FlayerDialogFragment flayerDialogFragment;

    public EventDetailFragment() {
        mFloatingActionButtonBehavior = new FloatingActionButtonBehavior(this);
        mHandler = new WeakHandler();
        mMapListener = new MapListener() {
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
    }

    @NonNull
    public static EventDetailFragment getInstance() {
        return new EventDetailFragment();
    }

    private float LOCATION_MIN_UPDATE_DISTANCE() {
        return getResources().getInteger(R.integer.location_min_update_distance);
    }

    private long LOCATION_MIN_UPDATE_TIME() {
        return getResources().getInteger(R.integer.location_min_update_time);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEventModel = NowApplication.getInstance().getEventModel();
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
        mMapView.setUseDataConnection(NowApplication.getInstance().getState() == ONLINE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMapView.setVisibility(View.INVISIBLE);
        mHandler.postDelayed(() -> {
            if (isResumed()) {
                mMapView.setVisibility(View.VISIBLE);
                createMap();
                if (!TextUtils.isEmpty(mEvent.flayer)) {
                    showHand();
                    mShakeDetector = new ShakeDetector(EventDetailFragment.this);
                    mShakeDetector.start((SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE));
                }
            }
        }, getResources().getInteger(R.integer.slide_animation_duration) * 3 / 2);
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
        NowApplication.getInstance().registerForAppChangeStateNotification(mChangeApplicationStateReceiver);

        initEventToCalendarLoader(EventToCalendarLoader.CHECK);

        initEventApiExecutor(EventApiLoader.METHOD_OBTAIN_STATS, mEvent.id);

        mFloatingActionButtonBehavior.activate();

        displayEventStats();

        //init advert::init load
        boolean advertPossibility = arg.getBoolean(KEY_ADVERT_POSSIBILITY, true);
        if (advertPossibility) {
            mEventAdvert = mEventModel.generateAdvertExcludeByEventId(mEvent.id);
            if (mEventAdvert != null) {
                obtainAdvertCallTime = System.currentTimeMillis();
                initEventApiExecutor(EventApiLoader.METHOD_OBTAIN_EVENT, mEventAdvert.eventId);
            }
        }

        mPosterThumbView.setOnClickListener(this);
        CoordinatorLayout.LayoutParams fabLayoutParams = (CoordinatorLayout.LayoutParams) mFloatingActionButton.getLayoutParams();
        fabLayoutParams.setBehavior(mFloatingActionButtonBehavior);

        mSiteButton.setOnClickListener(this);
        mPhoneButton.setOnClickListener(this);
        mMapViewContainer.findViewById(R.id.zoom_minus).setOnClickListener(this);
        mMapViewContainer.findViewById(R.id.zoom_plus).setOnClickListener(this);
        mMapViewContainer.findViewById(R.id.place_group).setOnClickListener(this);
        mMapViewContainer.findViewById(R.id.my_location).setOnClickListener(this);

        mFloatingActionButton.setOnClickListener(this);
        mMapViewContainer.findViewById(R.id.map_scroll_fake_view).setOnTouchListener((view, event) -> {
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
        });
    }

    private void showHand() {
        if (isResumed()) {
            Glide.with(this).load(R.raw.hand_shake).dontTransform().dontAnimate().into(mHandView);
        }
    }

    private void showAdvertBlockWithDelay(long delay) {
        if (isResumed()) {
            final Context context = getActivity().getApplicationContext();
            mTimer = new Timer();
            mTimer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(() -> {
                                expand(mAdvertBlock);
                                ImageView advertIcon = (ImageView) mAdvertBlock.findViewById(R.id.advert_icon_view);
                                Glide.with(context).load(mEventAdvert.logoThumb).into(advertIcon);
                                TextView advertTextView = (TextView) mAdvertBlock.findViewById(R.id.advert_text_view);
                                String advertMessage = TextUtils.join(" ", new String[]{getResources().getString(R.string.attention_short), EventsModel.getStartTimeAliasForAdvert(context, mEventLinkToAdvert), mEventAdvert.text});
                                advertTextView.setText(advertMessage);
                                mAdvertBlock.setOnClickListener(EventDetailFragment.this);
                            });
                        }
                    }, delay
            );
        }
    }

    private void expand(@NonNull final View view) {
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
        animation.setDuration((int) (targetHeight / getResources().getDisplayMetrics().density) * 4);
        view.startAnimation(animation);
    }

    private void initEventApiExecutor(int method, int eventId) {
        Bundle args = new Bundle();
        args.putInt(EventApiLoader.KEY_METHOD, method);
        args.putInt(EventApiLoader.KEY_EVENT_ITEM_ID, eventId);
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        loaderManager.initLoader(API_EXECUTOR_GROUP_ID + method, args, this);
    }

    private void initEventToCalendarLoader(int method) {
        Bundle args = new Bundle();
        args.putInt(EventToCalendarLoader.KEY_METHOD, method);
        args.putInt(EventToCalendarLoader.KEY_EVENT_ITEM_ID, mEvent.id);
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        loaderManager.initLoader(CALENDAR_LOADER_ID, args, this);
    }

    @Nullable
    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        Context context = getActivity().getApplicationContext();
        Loader loader = null;
        switch (id) {
            case CALENDAR_LOADER_ID:

                mEventToCalendarLink = null;
                loader = new EventToCalendarLoader(context, args);

                break;

            case API_EXECUTOR_GROUP_ID + EventApiLoader.METHOD_OBTAIN_STATS:
            case API_EXECUTOR_GROUP_ID + EventApiLoader.METHOD_LIKE:
            case API_EXECUTOR_GROUP_ID + EventApiLoader.METHOD_OBTAIN_EVENT:

                loader = new EventApiLoader(context, args);

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
        setMenuVisible(R.id.action_remove_from_calendar, calendarLinkIsPresent, menu);
        setMenuVisible(R.id.action_show_in_calendar, calendarLinkIsPresent, menu);
        setMenuVisible(R.id.action_add_to_calendar, !calendarLinkIsPresent, menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onLoadFinished(@NonNull Loader loader, @NonNull Object result) {
        if (isResumed()) {
            int id = loader.getId();
            switch (id) {
                case CALENDAR_LOADER_ID:

                    Pair<Integer, EventToCalendarLink> calendarResult = (Pair<Integer, EventToCalendarLink>) result;
                    int messageId = 0;
                    mEventToCalendarLink = calendarResult.second;
                    if (calendarResult.first == EventToCalendarLoader.ADD) {
                        messageId = R.string.add_to_calendar_message;
                    } else if (calendarResult.first == EventToCalendarLoader.REMOVE) {
                        messageId = R.string.remove_from_calendar_message;
                    }
                    if (messageId != 0) {
                        boolean doOverlay = mFloatingActionButtonBehavior.bottomViewIsOverlaped();
                        if (doOverlay) {
                            mFloatingActionButtonBehavior.allowMoveOnScroll(false);
                        }
                        mFloatingActionButtonBehavior.allowMoveOnSnackbarShow(doOverlay);
                        Snackbar snackbar = Snackbar.make(mFloatingActionButton, messageId, Snackbar.LENGTH_LONG);
                        snackbar.setActionTextColor(EventsModel.getCategoryColor(mEvent.category));
                        if (calendarResult.first == EventToCalendarLoader.ADD) {
                            snackbar.setAction(R.string.open_calendar_message, view -> {
                                openCalendarApplication();
                            });
                        }
                        snackbar.show();
                        mHandler.postDelayed(() ->
                                mFloatingActionButtonBehavior.allowMoveOnScroll(true), 3250);
                    }

                    break;

                case API_EXECUTOR_GROUP_ID + EventApiLoader.METHOD_OBTAIN_STATS:

                    displayEventStats();

                    break;

                case API_EXECUTOR_GROUP_ID + EventApiLoader.METHOD_LIKE:

                    initEventApiExecutor(EventApiLoader.METHOD_OBTAIN_STATS, mEvent.id);
                    ViewCompat.animate(mFloatingActionButton).scaleX(0).scaleY(0).setInterpolator(new FastOutSlowInInterpolator());

                    break;

                case API_EXECUTOR_GROUP_ID + EventApiLoader.METHOD_OBTAIN_EVENT:

                    mEventLinkToAdvert = ((EventApiCallResult) result).event;
                    showAdvertBlockWithDelay(Math.max(0, SHOW_ADVERT_DELAY - (System.currentTimeMillis() - obtainAdvertCallTime)));

                    break;

                default:
                    break;
            }
            getActivity().getSupportLoaderManager().destroyLoader(id);
        }
    }

    private void displayEventStats() {
        Resources resources = getResources();
        EventStats eventStats = mEventModel.obtainEventStatsByEventId(mEvent.id);
        if (eventStats != null) {
            boolean myLike = eventStats.myLikeStatus != 0 || eventStats.likedByMe;
            mViewsView.setText(eventStats.getViews());
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
            mFloatingActionButton.setVisibility(myLike ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void setMenuVisible(int id, boolean visible, @Nullable Menu menu) {
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
        NowApplication.getInstance().unregisterForAppChangeStateNotification(mChangeApplicationStateReceiver);
        mFloatingActionButtonBehavior.deactivate();
        if (myLocationOverLay != null) {
            myLocationOverLay.disableMyLocation();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        mPosterThumbView.setOnClickListener(null);
        mSiteButton.setOnClickListener(null);
        mPhoneButton.setOnClickListener(null);
        mMapViewContainer.findViewById(R.id.zoom_minus).setOnClickListener(null);
        mMapViewContainer.findViewById(R.id.zoom_plus).setOnClickListener(null);
        mMapViewContainer.findViewById(R.id.place_group).setOnClickListener(null);
        mMapViewContainer.findViewById(R.id.my_location).setOnClickListener(null);
        mMapViewContainer.findViewById(R.id.map_scroll_fake_view).setOnTouchListener(null);
        mFloatingActionButton.setOnClickListener(null);
        if (mShakeDetector != null) {
            mShakeDetector.stop();
        }
        super.onPause();
        //NowApplication.getInstance().getWatcher().watch(this);
    }

    private void createMap() {

        Context context = getActivity().getApplicationContext();

        //configure map view :: set parameters + add marker
        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.getController().setZoom(MAX_ZOOM);

        //configure map view overlay :: add my location marker
        myLocationOverLay = new MyLocationNewOverlay(context, mMapView);
        GpsMyLocationProvider gpsLocationProvider = new GpsMyLocationProvider(context);
        gpsLocationProvider.setLocationUpdateMinTime(LOCATION_MIN_UPDATE_TIME());
        gpsLocationProvider.setLocationUpdateMinDistance(LOCATION_MIN_UPDATE_DISTANCE());
        myLocationOverLay.enableMyLocation(gpsLocationProvider);
        myLocationOverLay.setDrawAccuracyEnabled(true);
        mMapView.getOverlays().add(myLocationOverLay);

        mMapView.setMapListener(mMapListener);

        checkZoomButtons();

        //configure map view overlay :: add map marker
        mEventLocationPoint = new GeoPoint(mEvent.lat, mEvent.lng);
        final ArrayList<OverlayItem> items = new ArrayList<>();
        OverlayItem overlayItem = new OverlayItem(null, null, mEventLocationPoint);
        overlayItem.setMarkerHotspot(OverlayItem.HotspotPlace.BOTTOM_CENTER);
        items.add(overlayItem);
        DefaultResourceProxyImpl resProxyImp = new DefaultResourceProxyImpl(getActivity());
        ItemizedIconOverlay markersOverlay = new ItemizedIconOverlay<>(items, getResources().getDrawable(R.drawable.ic_action_location), null, resProxyImp);
        mMapView.getOverlays().add(markersOverlay);
        mMapView.getController().setCenter(mEventLocationPoint);
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

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mEvent = getArguments().getParcelable(KEY_EVENT_ITEM);
        assert mEvent != null;
        mInflater = inflater;
        return constructInterface(inflater.inflate(R.layout.fragment_event_detail, container, false));
    }

    @NonNull
    private View constructInterface(@NonNull View view) {

        final Context context = getActivity().getApplicationContext();

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
            mPosterThumbView.setImageDrawable(getResources().getDrawable(R.drawable.event_poster_stub));
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
            ((ImageView) view.findViewById(R.id.category_type_view)).setImageDrawable(getResources().getDrawable(categoryDrawableId));
        }

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

                }
                mPhoneButton.setVisibility(View.VISIBLE);
                mPhoneButton.setText(mEvent.phone);
            }
        }
        Calendar calendar = Calendar.getInstance();
        //Strange
        calendar.setTimeZone(calendar.getTimeZone());
        calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(mEvent.startAt));
        dayDateView.setText(String.format("%d", calendar.get(Calendar.DAY_OF_MONTH)));
        monthView.setText(new DateFormatSymbols().getShortMonths()[calendar.get(Calendar.MONTH)].toLowerCase());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        dayOfWeekView.setText(getResources().getTextArray(R.array.day_of_week)[dayOfWeek]);
        createEventTimeTextBlock(context, timeView, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        createEventEntranceTextBlock(context, entranceView, mEvent.entrance);
        mDescriptionView.setText(mEvent.eventDescription);
        mMapViewContainer = view.findViewById(R.id.map_view_container);
        mMapView = (MapView) view.findViewById(R.id.map_view);
        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        mViewsIcon = (ImageView) view.findViewById(R.id.event_view_icon);
        mLikesIcon = (ImageView) view.findViewById(R.id.event_like_icon);
        mLikeContainer = view.findViewById(R.id.event_like_container);
        mAdvertBlock = view.findViewById(R.id.advert_block);
        mHandView = (ImageView) view.findViewById(R.id.hand_view);

        //configure map view container :: set height - half of screen height
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        mMapViewContainer.getLayoutParams().height = (height + getResources().getDimensionPixelOffset(R.dimen.logo_max_height)) / 2;

        createEventPlaceTextBlock(context, placeNameView, mEvent.placeName, mEvent.address);

        //TODO: do not work
        mScrollView.scrollTo(0, 0);

        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_event);
        mFloatingActionButton.setBackgroundTintList(ColorStateList.valueOf(EventsModel.getCategoryColor(mEvent.category)));

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
        setMenuVisible(R.id.action_add_to_calendar, false, menu);
        setMenuVisible(R.id.action_remove_from_calendar, false, menu);
        setMenuVisible(R.id.action_show_in_calendar, false, menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_share:

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mEvent.name);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, mEvent.eventDescription);
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_title)));

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

    private void createEventPlaceTextBlock(@NonNull Context context, @NonNull TextView placeNameView, @NonNull String placeName, @NonNull String address) {
        SpannableString placeNameSpan = new SpannableString(placeName);
        placeNameSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.Place_Name), 0, placeName.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString addressSpan = new SpannableString(address);
        addressSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.Place_Address), 0, address.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        placeNameView.setText(address.equals(placeName) ? placeNameSpan : TextUtils.concat(placeNameSpan, "\n", addressSpan));
    }

    private void createEventEntranceTextBlock(@NonNull Context context, @NonNull TextView entranceView, @NonNull String entrance) {
        String title = getResources().getString(R.string.entrance_title);
        SpannableString titleSpan = new SpannableString(title);
        titleSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.EntranceTitle), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString entranceSpan = new SpannableString(entrance);
        entranceSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.Entrance), 0, entrance.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        entranceView.setText(TextUtils.concat(titleSpan, "\n", entranceSpan));
    }

    private void createEventTimeTextBlock(@NonNull Context context, @NonNull TextView timeView, int hour, int minute) {
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
        if (attrArray != null) {
            try {
                hourTextSize = attrArray.getDimensionPixelOffset(0, -1);
            } catch (RuntimeException exp) {
                //empty
            } finally {
                attrArray.recycle();
            }
        }

        SpannableString hourValueSpan = new SpannableString(hourTextValue);
        hourValueSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.HourText), 0, hourTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableStringBuilder minuteSpanBuilder = new SpannableStringBuilder(minuteTextValue);
        minuteSpanBuilder.setSpan(new RobotoTextAppearanceSpan(context, R.style.MinuteText), 0, minuteTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        minuteSpanBuilder.setSpan(TextViewUtils.getSuperscriptSpanAdjuster(context, hourTextValue, hourTextSize), 0, minuteTextValue.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence finalText = TextUtils.concat(hourValueSpan, minuteSpanBuilder);
        timeView.setText(finalText);
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    public void setEventAndAdvertPossibility(Event event, boolean advertPossibility) {
        Bundle arg = getArguments();
        if (arg == null) {
            arg = new Bundle();
        }
        arg.putParcelable(KEY_EVENT_ITEM, event);
        arg.putBoolean(KEY_ADVERT_POSSIBILITY, advertPossibility);
        setArguments(arg);
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        try {
            mEventPosterSelectListener = (IEventPosterSelectListener) getActivity();
        } catch (ClassCastException exp) {
            throw new ClassCastException(activity.toString()
                    + " must implement IEventPosterSelectListener");
        }
        try {
            mEventClickListener = (IEventClickListener) getActivity();
        } catch (ClassCastException exp) {
            throw new ClassCastException(activity.toString()
                    + " must implement IEventClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEventPosterSelectListener = null;
        mEventClickListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.setMapListener(null);
        unbindDrawables(mRootLayout);
    }

    @Override
    public void onClick(@NonNull View view) {
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
                }
                checkZoomButtons();

                break;

            case R.id.fab_event:

                EventStats eventStats = mEventModel.obtainEventStatsByEventId(mEvent.id);
                if (eventStats != null && eventStats.myLikeStatus == 0) {
                    initEventApiExecutor(EventApiLoader.METHOD_LIKE, mEvent.id);
                }

                break;

            case R.id.advert_block:

                mEventClickListener.onEventClick(mEventLinkToAdvert, true);
                mAdvertBlock.setOnClickListener(null);

                break;

            default:
                break;
        }
    }

    public void showFlayerDialog(String imageUrl) {
        if (flayerDialogFragment == null) {
            flayerDialogFragment = FlayerDialogFragment.getInstance(imageUrl);
        }
        if (!flayerDialogFragment.isShowsDialog()) {
            flayerDialogFragment.show(getActivity().getSupportFragmentManager(), "flayerDialog");
            flayerDialogFragment.setDialogListener(this);
        }
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
                .setPositiveButton(R.string.switch_on, (dialog1, which) -> {
                    mProgressWheel.setVisibility(View.VISIBLE);
                    Intent callGPSSettingIntent = new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(callGPSSettingIntent);
                }).setNegativeButton(R.string.no, null)
                .setCustomTitle(dialogTitleView);
        builder.setInverseBackgroundForced(true);
        dialogTitleTextView.setText(R.string.gps_is_off);
        dialogTextView.setText(R.string.gps_switch_on_ask);
        dialog = builder.create();
        dialog.show();
    }

    @Override
    public void hearShake() {
        if (!TextUtils.isEmpty(mEvent.flayer)) {
            showFlayerDialog(mEvent.flayer);
        }
    }

    @Override
    public void onShowDialog() {
        mHandView.setVisibility(View.INVISIBLE);
        ViewCompat.animate(mFloatingActionButton).scaleX(0).scaleY(0).setInterpolator(new FastOutSlowInInterpolator());
    }

    @Override
    public void onCloseDialog() {
        mHandView.setVisibility(View.VISIBLE);
        EventStats eventStats = mEventModel.obtainEventStatsByEventId(mEvent.id);
        if (eventStats != null && !eventStats.likedByMe) {
            ViewCompat.animate(mFloatingActionButton).scaleX(1).scaleY(1).setInterpolator(new FastOutSlowInInterpolator());
        }
        flayerDialogFragment.setDialogListener(null);
    }

    static class FloatingActionButtonBehavior extends FloatingActionButton.Behavior {

        private float mTranslationY;
        private boolean mAllowMoveOnScroll, mAllowMoveOnSnackbarShow;
        private WeakReference<EventDetailFragment> fragmentReference;

        private final ViewTreeObserver.OnGlobalLayoutListener floatingActionButtonLayoutListener = this::validatePosition;

        @NonNull
        private ViewTreeObserver.OnScrollChangedListener scrollListener = this::validatePosition;

        public FloatingActionButtonBehavior(EventDetailFragment fragment) {
            mAllowMoveOnScroll = true;
            mAllowMoveOnSnackbarShow = true;
            fragmentReference = new WeakReference<>(fragment);
        }

        public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull FloatingActionButton child, @NonNull View dependency) {
            EventDetailFragment fragment = fragmentReference.get();
            if (fragment != null && fragment.isResumed()) {
                updateFabTranslationForSnackbar(parent, child, dependency);
            }
            return false;
        }

        private void updateFabTranslationForSnackbar(@NonNull CoordinatorLayout parent, @NonNull FloatingActionButton fab, @NonNull View snackbar) {
            if (mAllowMoveOnSnackbarShow) {
                float translationY = this.getFabTranslationYForSnackbar(parent, fab);
                if (translationY != this.mTranslationY) {
                    ViewCompat.animate(fab).cancel();
                    if (Math.abs(translationY - this.mTranslationY) == (float) snackbar.getHeight()) {
                        ViewCompat.animate(fab).translationY(translationY).setInterpolator(new FastOutSlowInInterpolator());
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

        private float getFabTranslationYForSnackbar(@NonNull CoordinatorLayout parent, @NonNull FloatingActionButton fab) {
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
            EventDetailFragment fragment = fragmentReference.get();
            if (fragment != null) {
                fragment.mFloatingActionButton.getViewTreeObserver().addOnGlobalLayoutListener(floatingActionButtonLayoutListener);
                fragment.mScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
                validatePosition();
            }
        }

        public void deactivate() {
            EventDetailFragment fragment = fragmentReference.get();
            if (fragment != null) {
                fragment.mFloatingActionButton.getViewTreeObserver().removeGlobalOnLayoutListener(floatingActionButtonLayoutListener);
                fragment.mScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
            }
        }

        private void validatePosition() {
            EventDetailFragment fragment = fragmentReference.get();
            if (fragment != null && fragment.isResumed()) {
                float appWorkAreaHeight = fragment.mRootLayout.getHeight();
                float appHeight = getAppHeight();
                int fabHeight = fragment.mFloatingActionButton.getHeight();
                float bottomViewBottom = getBottomViewBottom();
                int space = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? 0 : fragment.getResources().getDimensionPixelOffset(R.dimen.large_space);
                if (mAllowMoveOnScroll) {
                    float y;
                    if (appHeight < bottomViewBottom) {
                        y = appWorkAreaHeight;
                    } else {
                        y = appWorkAreaHeight - (appHeight - bottomViewBottom);
                    }
                    fragment.mFloatingActionButton.setY(y - fabHeight - space);
                    fragment.mHandView.setY(y - fragment.mHandView.getHeight() / 2 - fragment.getResources().getDimension(R.dimen.big_space) - fabHeight / 2 * (fragment.mFloatingActionButton.getVisibility() == View.VISIBLE ? 2 : 1));
                }
                fragment.mDescriptionView.setPadding(0, 0, 0, fragment.mFloatingActionButton.getVisibility() == View.VISIBLE ? fabHeight / 3 * 2 : 0);
            }
        }

        private float getAppHeight() {
            EventDetailFragment fragment = fragmentReference.get();
            if (fragment != null) {
                float appWorkAreaHeight = fragment.mRootLayout.getHeight();
                int[] appWorkAreaCords = new int[2];
                fragment.mRootLayout.getLocationOnScreen(appWorkAreaCords);
                return appWorkAreaCords[1] + appWorkAreaHeight;
            }
            return 0;
        }

        private float getBottomViewBottom() {
            EventDetailFragment fragment = fragmentReference.get();
            if (fragment != null) {
                View bottomView = fragment.mMapViewContainer;
                if (fragment.mPhoneButton.getVisibility() == View.VISIBLE) {
                    bottomView = fragment.mPhoneButton;
                } else if (fragment.mSiteButton.getVisibility() == View.VISIBLE) {
                    bottomView = fragment.mSiteButton;
                }
                int[] bottomViewCords = new int[2];
                bottomView.getLocationOnScreen(bottomViewCords);
                return bottomViewCords[1];
            }
            return 0;
        }

        public boolean bottomViewIsOverlaped() {
            return doBottomViewOverlap(true);
        }

        private boolean doBottomViewOverlap(boolean withSnackBar) {
            EventDetailFragment fragment = fragmentReference.get();
            if (fragment != null) {
                return getAppHeight() < getBottomViewBottom() + (withSnackBar ? 1 : 0) * fragment.getResources().getDimensionPixelOffset(R.dimen.snack_bar_height);
            }
            return false;
        }

        public void allowMoveOnScroll(boolean value) {
            mAllowMoveOnScroll = value;
            validatePosition();
        }

        public void allowMoveOnSnackbarShow(boolean value) {
            mAllowMoveOnSnackbarShow = value;
        }
    }
}
