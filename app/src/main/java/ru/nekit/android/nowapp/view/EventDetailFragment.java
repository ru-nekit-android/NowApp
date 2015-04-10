package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemPosterSelectListener;
import ru.nekit.android.nowapp.utils.TextViewUtils;

@SuppressWarnings("ResourceType")
public class EventDetailFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "ru.nekit.android.event_detail_fragment";

    private static final String ARG_EVENT_ITEM = "ru.nekit.android.event_item";
    private static final int MAX_ZOOM = 19;

    private MapView mMapView;
    private EventItem mEventItem;
    private IEventItemPosterSelectListener mEventItemPosterSelectListener;
    private ProgressWheel mProgressWheel;
    private GeoPoint mGeoPoint;

    public EventDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        createMap();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void createMap() {
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mGeoPoint = new GeoPoint(mEventItem.lat, mEventItem.lng);
        mMapView.getController().setZoom(MAX_ZOOM);
        mMapView.getController().setCenter(mGeoPoint);
        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        OverlayItem marker = new OverlayItem(null, null, mGeoPoint);
        marker.setMarkerHotspot(OverlayItem.HotspotPlace.BOTTOM_CENTER);
        items.add(marker);
        Drawable newMarker = this.getResources().getDrawable(R.drawable.map_marker);
        DefaultResourceProxyImpl resProxyImp = new DefaultResourceProxyImpl(getActivity().getApplicationContext());
        ItemizedIconOverlay markersOverlay = new ItemizedIconOverlay<OverlayItem>(items, newMarker, null, resProxyImp);
        mMapView.getOverlays().add(markersOverlay);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return constructInterface(inflater.inflate(R.layout.fragment_event_detail, container, false));
    }

    private View constructInterface(View view) {

        Bundle arg = getArguments();
        Context context = getActivity();
        mEventItem = arg.getParcelable(ARG_EVENT_ITEM);

        TextView titleView = (TextView) view.findViewById(R.id.title_view);
        ArrayList<String> eventNameArray = ru.nekit.android.nowapp.utils.StringUtil.wrapText(mEventItem.name.toUpperCase());
        titleView.setLines(eventNameArray.size());
        titleView.setText(TextUtils.join("\n", eventNameArray));
        ImageView posterThumbView = (ImageView) view.findViewById(R.id.poster_thumb_view);
        mProgressWheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);
        Glide.with(context).load(mEventItem.posterThumb).listener(new RequestListener<String, GlideDrawable>() {
            @Override
            public boolean onException(Exception exp, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                mProgressWheel.setVisibility(View.GONE);
                return false;
            }
        }).into(posterThumbView);
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
                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
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

        posterThumbView.setOnClickListener(this);

        TextView dayDateView = (TextView) view.findViewById(R.id.day_date_view);
        TextView monthView = (TextView) view.findViewById(R.id.month_view);
        TextView dayOfWeekView = (TextView) view.findViewById(R.id.day_of_week_view);
        TextView timeView = (TextView) view.findViewById(R.id.time_view);
        TextView entranceView = (TextView) view.findViewById(R.id.entrance_view);
        TextView descriptionView = (TextView) view.findViewById(R.id.description_view);
        TextView addressView = (TextView) view.findViewById(R.id.address_view);
        Button phoneButton = (Button) view.findViewById(R.id.phone_button);
        Button siteButton = (Button) view.findViewById(R.id.site_button);
        addressView.setOnClickListener(this);
        phoneButton.setVisibility(View.GONE);
        siteButton.setVisibility(View.GONE);
        if (!"".equals(mEventItem.site)) {
            siteButton.setVisibility(View.VISIBLE);
            siteButton.setOnClickListener(this);
            siteButton.setText(mEventItem.site);
            siteButton.setTransformationMethod(null);
        }
        if (!"".equals(mEventItem.phone)) {
            if (mEventItem.phone.length() > 1) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                    phoneButton.setEnabled(false);
                } else {
                    phoneButton.setEnabled(true);
                    phoneButton.setOnClickListener(this);
                }
                phoneButton.setVisibility(View.VISIBLE);
                phoneButton.setText(mEventItem.phone);
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(mEventItem.date));
        dayDateView.setText(String.format("%d", calendar.get(Calendar.DAY_OF_MONTH)));
        monthView.setText(new DateFormatSymbols().getMonths()[calendar.get(Calendar.MONTH)].toLowerCase());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        dayOfWeekView.setText(getResources().getTextArray(R.array.day_of_week)[dayOfWeek]);

        calendar.set(Calendar.SECOND, EventItemsModel.getCurrentTimeFromEventInSeconds(mEventItem));
        createEventTimeTextBlock(context, timeView, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

        createEventEntranceTextBlock(context, entranceView, mEventItem.entrance);

        descriptionView.setText(mEventItem.eventDescription);

        mMapView = (MapView) view.findViewById(R.id.map_view);

        final ScrollView scrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        View mapScrollFakeView = view.findViewById(R.id.map_scroll_fake_view);

        mapScrollFakeView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    case MotionEvent.ACTION_UP:
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    default:
                        return true;
                }
            }
        });

        addressView.setText(mEventItem.address);

        return view;
    }

    private void createEventEntranceTextBlock(Context context, TextView entranceView, String entrance) {
        String title = "ВХОД:";
        SpannableString titleSpan = new SpannableString(title);
        titleSpan.setSpan(new TextAppearanceSpan(context, R.style.EntranceTitleText), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString entranceSpan = new SpannableString(entrance);
        entranceSpan.setSpan(new TextAppearanceSpan(context, R.style.EntranceText), 0, entrance.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        hourValueSpan.setSpan(new TextAppearanceSpan(context, R.style.HourText), 0, hourTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableStringBuilder minuteSpanBuilder = new SpannableStringBuilder(minuteTextValue);
        minuteSpanBuilder.setSpan(new TextAppearanceSpan(context, R.style.MinuteText), 0, minuteTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        minuteSpanBuilder.setSpan(TextViewUtils.getSuperscriptSpanAdjuster(context, hourTextValue, hourTextSize), 0, minuteTextValue.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence finalText = TextUtils.concat(hourValueSpan, minuteSpanBuilder);
        timeView.setText(finalText);
    }

    private void setEventItem(EventItem eventItem) {
        Bundle arg = new Bundle();
        arg.putParcelable(ARG_EVENT_ITEM, eventItem);
        setArguments(arg);
    }


    public void updateEventItem(EventItem eventItem) {
        Bundle arg = getArguments();
        arg.putParcelable(ARG_EVENT_ITEM, eventItem);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEventItemPosterSelectListener = (IEventItemPosterSelectListener) getActivity();
        } catch (ClassCastException e) {
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

            case R.id.address_view:

                mMapView.getController().setZoom(MAX_ZOOM);
                mMapView.getController().animateTo(mGeoPoint);

                break;

            case R.id.poster_thumb_view:

                mEventItemPosterSelectListener.onEventItemPosterSelect(mEventItem.posterOriginal);

                break;

            default:
        }
    }

    public static EventDetailFragment getInstance(EventItem eventItem) {
        EventDetailFragment fragment = new EventDetailFragment();
        fragment.setEventItem(eventItem);
        return fragment;
    }
}
