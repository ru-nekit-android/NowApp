package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.devspark.robototextview.util.RobotoTypefaceManager;
import com.getbase.floatingactionbutton.FloatingActionButton;
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
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.model.EventToCalendarLoader;
import ru.nekit.android.nowapp.model.db.vo.EventToCalendarLink;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemPosterSelectListener;
import ru.nekit.android.nowapp.utils.RobotoTextAppearanceSpan;
import ru.nekit.android.nowapp.utils.TextViewUtils;
import ru.nekit.android.nowapp.widget.OnSwipeTouchListener;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

@SuppressWarnings("ResourceType")
public class EventDetailFragment extends Fragment implements View.OnClickListener, LoaderManager.LoaderCallbacks<EventToCalendarLink> {

    public static final String TAG = "ru.nekit.android.event_detail_fragment";

    private static final int LOADER_ID = 0;

    private static final String EVENT_ITEM_KEY = "ru.nekit.android.event_item";
    private static final int MAX_ZOOM = 19;

    private MapView mMapView;
    private EventItem mEventItem;
    private IEventItemPosterSelectListener mEventItemPosterSelectListener;
    private ProgressWheel mProgressWheel;
    private GeoPoint mEventLocationPoint;
    private RelativeLayout mMapViewContainer;
    private ImageView mPosterThumbView;
    private boolean mPosterViewIsEmpty;
    private BroadcastReceiver mChangeApplicationStateReceiver;
    private MyLocationNewOverlay myLocationOverLay;
    private LocationManager mLocationManager;
    private FloatingActionButton mFloatingActionButton;
    private MapListener mMapListener = new MapListener() {
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

    private ViewTreeObserver.OnScrollChangedListener scrollListener = new ViewTreeObserver.OnScrollChangedListener() {

        @Override
        public void onScrollChanged() {
            updateFloatingActionButtonPosition();
        }
    };

    ViewTreeObserver.OnGlobalLayoutListener floatingActionButtonLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

        @Override
        public void onGlobalLayout() {
            updateFloatingActionButtonPosition();
        }

    };

    private ScrollView mScrollView;
    private TextView mDescriptionView;
    private Button mPhoneButton;
    private Button mSiteButton;
    private EventToCalendarLink mEventToCalendarLink;

    public EventDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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
        FragmentActivity activity = getActivity();
        ((ActionBarActivity) activity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        applyApplicationState();
        LocalBroadcastManager.getInstance(activity).registerReceiver(mChangeApplicationStateReceiver, new IntentFilter(NowApplication.CHANGE_APPLICATION_STATE));

        GpsMyLocationProvider gpsLocationProvider = new GpsMyLocationProvider(activity);
        gpsLocationProvider.setLocationUpdateMinTime(TimeUnit.SECONDS.toMillis(5));
        gpsLocationProvider.setLocationUpdateMinDistance(50);
        myLocationOverLay.enableMyLocation(gpsLocationProvider);
        myLocationOverLay.setDrawAccuracyEnabled(true);
        mFloatingActionButton.getViewTreeObserver().addOnGlobalLayoutListener(floatingActionButtonLayoutListener);
        mScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        initEventToCalendarLoader(EventToCalendarLoader.CHECK);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mChangeApplicationStateReceiver);
        mScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        mFloatingActionButton.getViewTreeObserver().removeGlobalOnLayoutListener(floatingActionButtonLayoutListener);
        myLocationOverLay.disableMyLocation();
    }

    private void updateFloatingActionButtonPosition() {
        View bottomView = mDescriptionView;
        if (mPhoneButton.getVisibility() == View.VISIBLE) {
            bottomView = mPhoneButton;
        } else if (mSiteButton.getVisibility() == View.VISIBLE) {
            bottomView = mSiteButton;
        }
        int scrollY = mScrollView.getScrollY();
        int fabHeight = mFloatingActionButton.getHeight();
        float screenBottom = mScrollView.getHeight();
        float bottomViewBottom = bottomView.getY() - scrollY;
        if (screenBottom < bottomViewBottom) {
            mFloatingActionButton.setY(screenBottom - fabHeight);
        } else {
            mFloatingActionButton.setY(bottomViewBottom - fabHeight);
        }
        mDescriptionView.setPadding(0, 0, 0, fabHeight - (int) getActivity().getResources().getDimension(R.dimen.normal_space));
    }

    private void createMap() {

        Context context = getActivity();

        //configure map view container :: set height - half of screen height
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        mMapViewContainer.getLayoutParams().height = height / 2;

        //configure map view :: set parameters + add marker
        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mEventLocationPoint = new GeoPoint(mEventItem.lat, mEventItem.lng);
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
        mEventItem = arg.getParcelable(EVENT_ITEM_KEY);
        return constructInterface(inflater.inflate(R.layout.fragment_event_detail, container, false));
    }

    private View constructInterface(View view) {

        Bundle arg = getArguments();
        final Context context = getActivity();
        mEventItem = arg.getParcelable(EVENT_ITEM_KEY);

        TextView titleView = (TextView) view.findViewById(R.id.title_view);
        ArrayList<String> eventNameArray = ru.nekit.android.nowapp.utils.StringUtil.wrapText(mEventItem.name.toUpperCase());
        titleView.setLines(eventNameArray.size());
        titleView.setText(TextUtils.join("\n", eventNameArray));
        mPosterThumbView = (ImageView) view.findViewById(R.id.poster_thumb_view);
        mProgressWheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);
        mProgressWheel.setVisibility(View.VISIBLE);
        if (mEventItem.posterThumb != null && !"".equals(mEventItem.posterThumb)) {
            Glide.with(context).load(mEventItem.posterThumb).listener(new RequestListener<String, GlideDrawable>() {
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
        String logoThumb = mEventItem.logoThumb;
        final ImageView logoThumbView = (ImageView) view.findViewById(R.id.logo_view);
        final TextView placeView = (TextView) view.findViewById(R.id.place_view);
        if ("".equals(logoThumb)) {
            logoThumbView.setVisibility(View.GONE);
            placeView.setVisibility(View.VISIBLE);
            placeView.setText(mEventItem.placeName);
        } else {
            logoThumbView.setVisibility(View.VISIBLE);
            placeView.setVisibility(View.GONE);
            Glide.with(context).load(logoThumb).listener(new RequestListener<String, GlideDrawable>() {

                @Override
                public boolean onException(Exception exp, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                    logoThumbView.setVisibility(View.GONE);
                    placeView.setVisibility(View.VISIBLE);
                    placeView.setText(mEventItem.placeName);
                    return true;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    return false;
                }
            }).into(logoThumbView);
        }
        int categoryDrawableId = EventItemsModel.getCategoryBigDrawable(mEventItem.category);
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
        mPhoneButton = (Button) view.findViewById(R.id.phone_button);
        mSiteButton = (Button) view.findViewById(R.id.site_button);
        mPhoneButton.setVisibility(View.GONE);
        mSiteButton.setVisibility(View.GONE);
        if (!"".equals(mEventItem.site)) {
            mSiteButton.setVisibility(View.VISIBLE);
            mSiteButton.setOnClickListener(this);
            mSiteButton.setText(mEventItem.site);
            mSiteButton.setTransformationMethod(null);
        }
        if (!"".equals(mEventItem.phone)) {
            if (mEventItem.phone.length() > 1) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                    mPhoneButton.setEnabled(false);
                } else {
                    mPhoneButton.setEnabled(true);
                    mPhoneButton.setOnClickListener(this);
                }
                mPhoneButton.setVisibility(View.VISIBLE);
                mPhoneButton.setText(mEventItem.phone);
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(mEventItem.date));
        dayDateView.setText(String.format("%d", calendar.get(Calendar.DAY_OF_MONTH)));
        monthView.setText(new DateFormatSymbols().getShortMonths()[calendar.get(Calendar.MONTH)].toLowerCase());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        dayOfWeekView.setText(getResources().getTextArray(R.array.day_of_week)[dayOfWeek]);
        calendar.set(Calendar.SECOND, (int) EventItemsModel.getEventStartTimeInSeconds(mEventItem));
        createEventTimeTextBlock(context, timeView, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        createEventEntranceTextBlock(context, entranceView, mEventItem.entrance);
        mDescriptionView.setText(mEventItem.eventDescription);
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

        view.setOnTouchListener(new OnSwipeTouchListener(context) {
            public void onSwipeRight() {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.zoom_minus).setOnClickListener(this);
        view.findViewById(R.id.zoom_plus).setOnClickListener(this);
        view.findViewById(R.id.event_location).setOnClickListener(this);
        view.findViewById(R.id.my_location).setOnClickListener(this);

        TextView addressButton = (TextView) view.findViewById(R.id.address_view);
        addressButton.setText(mEventItem.address);
        addressButton.setTransformationMethod(null);
        addressButton.setOnClickListener(this);

        mMapViewContainer = (RelativeLayout) view.findViewById(R.id.map_view_container);
        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab);
        mFloatingActionButton.setColorNormal(EventItemsModel.getCategoryColor(mEventItem.category));

        mFloatingActionButton.setOnClickListener(this);

        mScrollView.scrollTo(0, 0);

        return view;
    }

    private void createEventEntranceTextBlock(Context context, TextView entranceView, String entrance) {
        String title = "ВХОД:";
        SpannableString titleSpan = new SpannableString(title);
        titleSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.EntranceTitleText), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString entranceSpan = new SpannableString(entrance);
        entranceSpan.setSpan(new RobotoTextAppearanceSpan(context, R.style.EntranceText), 0, entrance.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence finalText = TextUtils.concat(titleSpan, "\n", entranceSpan);
        entranceView.setText(finalText);
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

    private void setEventItem(EventItem eventItem) {
        Bundle arg = new Bundle();
        arg.putParcelable(EVENT_ITEM_KEY, eventItem);
        setArguments(arg);
    }


    public void updateEventItem(EventItem eventItem) {
        Bundle arg = getArguments();
        arg.putParcelable(EVENT_ITEM_KEY, eventItem);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEventItemPosterSelectListener = (IEventItemPosterSelectListener) getActivity();
        } catch (ClassCastException exp) {
            throw new ClassCastException(activity.toString()
                    + " must implement IEventItemPosterSelectListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEventItemPosterSelectListener = null;
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.phone_button:
                intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + mEventItem.phone));
                startActivity(intent);
                break;

            case R.id.site_button:

                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(mEventItem.site));
                startActivity(intent);
                break;

            case R.id.event_location:

                //mMapView.getController().setZoom(MAX_ZOOM);
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
                    mEventItemPosterSelectListener.onEventItemPosterSelect(mEventItem.posterOriginal);
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

            case R.id.fab:

                if (mEventToCalendarLink != null) {
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, mEventToCalendarLink.getCalendarEventID()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(Intent.createChooser(intent, "XUI"));
                } else {
                    initEventToCalendarLoader(EventToCalendarLoader.ADD);
                }

                break;

            default:
        }
    }

    private void initEventToCalendarLoader(int method) {
        Bundle loaderArgs = new Bundle();
        loaderArgs.putInt(EventToCalendarLoader.METHOD_KEY, method);
        loaderArgs.putParcelable(EventToCalendarLoader.EVENT_ITEM_KEY, mEventItem);
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        final Loader<EventToCalendarLink> loader = loaderManager.getLoader(LOADER_ID);
        if (loader != null) {
            loaderManager.restartLoader(LOADER_ID, loaderArgs, this);
        } else {
            loaderManager.initLoader(LOADER_ID, loaderArgs, this);
        }
    }

    private void showGPSDisabledDialog() {
        Context context = getActivity();
        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.gps_is_off)
                .content(R.string.gps_switch_on_ask)
                .positiveText(R.string.switch_on)
                .negativeText(R.string.no)
                .typeface(RobotoTypefaceManager.obtainTypeface(context, RobotoTypefaceManager.Typeface.ROBOTO_REGULAR),
                        RobotoTypefaceManager.obtainTypeface(context, RobotoTypefaceManager.Typeface.ROBOTO_REGULAR))
                .customView(R.layout.text_view_content, false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mProgressWheel.setVisibility(View.VISIBLE);
                        Intent callGPSSettingIntent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                })
                .disableDefaultFonts()
                .cancelable(true)
                .show();
        View view = dialog.getCustomView();
        ((TextView) view.findViewById(R.id.text_view)).setText(R.string.gps_switch_on_ask);
    }

    public static EventDetailFragment getInstance(EventItem eventItem) {
        EventDetailFragment fragment = new EventDetailFragment();
        fragment.setEventItem(eventItem);
        return fragment;
    }


    @Override
    public Loader<EventToCalendarLink> onCreateLoader(int id, Bundle args) {
        mFloatingActionButton.setEnabled(false);
        mEventToCalendarLink = null;
        EventToCalendarLoader loader = new EventToCalendarLoader(getActivity(), args);
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<EventToCalendarLink> loader, EventToCalendarLink result) {
        mEventToCalendarLink = result;
        mFloatingActionButton.setEnabled(true);
        if (result == null) {
            mFloatingActionButton.setIcon(R.drawable.ic_action_calendar);
        } else {
            mFloatingActionButton.setIcon(R.drawable.ic_action_calendar_added);
        }
    }

    @Override
    public void onLoaderReset(Loader<EventToCalendarLink> loader) {

    }


}
