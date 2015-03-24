package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.utils.TextViewUtils;

@SuppressWarnings("ResourceType")
public class EventItemDetailFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "ru.nekit.android.event_item_detail_fragment";

    private static final String ARG_EVENT_ITEM = "ru.nekit.android.event_item";

    private GoogleMap mMap;
    private MapView mMapView;
    private EventItem mEventItem;
    private boolean mHasMap;

    public EventItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setRetainInstance(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        createMap(getActivity(), savedInstanceState, mEventItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arg = getArguments();
        Context context = getActivity();
        mEventItem = arg.getParcelable(ARG_EVENT_ITEM);
        View view = inflater.inflate(R.layout.fragment_event_item_detail, container, false);
        TextView titleView = (TextView) view.findViewById(R.id.title_view);
        titleView.setText(mEventItem.name.toUpperCase());
        Glide.with(getActivity()).load(mEventItem.posterThumb).into((ImageView) view.findViewById(R.id.poster_thumb_view));
        String logoThumb = mEventItem.logoThumb;
        ImageView logoThumbView = (ImageView) view.findViewById(R.id.logo_view);
        TextView placeView = (TextView) view.findViewById(R.id.place_view);
        if ("".equals(logoThumb)) {
            logoThumbView.setVisibility(View.GONE);
            placeView.setVisibility(View.VISIBLE);
            placeView.setText(mEventItem.placeName);
        } else {
            logoThumbView.setVisibility(View.VISIBLE);
            placeView.setVisibility(View.GONE);
            Glide.with(context).load(mEventItem.logoThumb).into(logoThumbView);
        }
        int categoryDrawableId = EventItemsModel.getCategoryBigDrawable(mEventItem.category);
        if (categoryDrawableId != 0) {
            ((ImageView) view.findViewById(R.id.category_type_view)).setImageDrawable(getActivity().getResources().getDrawable(categoryDrawableId));
        }

        TextView dayDateView = (TextView) view.findViewById(R.id.day_date_view);
        TextView monthView = (TextView) view.findViewById(R.id.month_view);
        TextView dayOfWeekView = (TextView) view.findViewById(R.id.day_of_week_view);
        TextView timeView = (TextView) view.findViewById(R.id.time_view);
        TextView entranceView = (TextView) view.findViewById(R.id.entrance_view);
        TextView descriptionView = (TextView) view.findViewById(R.id.description_view);
        Button phoneButton = (Button) view.findViewById(R.id.phone_button);
        Button emailButton = (Button) view.findViewById(R.id.email_button);
        phoneButton.setVisibility(View.GONE);
        emailButton.setVisibility(View.GONE);
        if (!"".equals(mEventItem.email)) {
            emailButton.setVisibility(View.VISIBLE);
            emailButton.setOnClickListener(this);
            emailButton.setText(mEventItem.email);
        }
        if (!"".equals(mEventItem.phone)) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {

            } else {
                phoneButton.setOnClickListener(this);
            }
            phoneButton.setVisibility(View.VISIBLE);
            phoneButton.setText(mEventItem.phone);
        }

        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(mEventItem.date * 1000);
        dayDateView.setText(String.format("%d", cl.get(Calendar.DAY_OF_MONTH)));
        monthView.setText(new DateFormatSymbols().getMonths()[cl.get(Calendar.MONTH)]);
        int dayOfWeek = cl.get(Calendar.DAY_OF_WEEK) - 1;
        dayOfWeekView.setText(getResources().getTextArray(R.array.day_of_week)[dayOfWeek]);
        createEventTimeTextBlock(context, timeView, cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE));

        createEventEntranceTextBlock(context, entranceView, mEventItem.entrance);

        descriptionView.setText(mEventItem.eventDescription);

        mMapView = (MapView) view.findViewById(R.id.map_view);

        return view;
    }


    private void createMap(Context context, Bundle savedInstanceState, EventItem eventitem) {
        boolean hasPlaceCoordinates = eventitem.lat != Double.NaN && eventitem.lng != Double.NaN;
        mHasMap = getView() != null && hasPlaceCoordinates;
        if (mHasMap) {
            mMapView.onCreate(savedInstanceState);
            mMap = mMapView.getMap();
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.setMyLocationEnabled(true);

            MapsInitializer.initialize(context);

            BitmapDescriptor defaultMarker =
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
            LatLng position = new LatLng(eventitem.lat, eventitem.lng);
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Some title here")
                    .snippet("Some description here")
                    .icon(defaultMarker));

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 15);
            mMap.moveCamera(cameraUpdate);
        }
    }

    private void createEventEntranceTextBlock(Context context, TextView entranceView, String entrance) {
        String title = "ВХОД:";
        SpannableString titleSpan = new SpannableString(title);
        titleSpan.setSpan(new TextAppearanceSpan(context, R.style.EntranceTitleTextStyle), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString entranceSpan = new SpannableString(entrance);
        entranceSpan.setSpan(new TextAppearanceSpan(context, R.style.EntranceTextStyle), 0, entrance.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        TypedArray attrArray = context.obtainStyledAttributes(R.style.HourTextStyle, R.styleable.StyleAttributes);
        int hourTextSize = 0;
        try {
            hourTextSize = attrArray.getDimensionPixelOffset(0, -1);
        } catch (RuntimeException exp) {
            //empty
        } finally {
            attrArray.recycle();
        }

        SpannableString hourValueSpan = new SpannableString(hourTextValue);
        hourValueSpan.setSpan(new TextAppearanceSpan(context, R.style.HourTextStyle), 0, hourTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableStringBuilder minuteSpanBuilder = new SpannableStringBuilder(minuteTextValue);
        minuteSpanBuilder.setSpan(new TextAppearanceSpan(context, R.style.MinuteTextStyle), 0, minuteTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        minuteSpanBuilder.setSpan(TextViewUtils.getSuperscriptSpanAdjuster(context, hourTextValue, hourTextSize), 0, minuteTextValue.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence finalText = TextUtils.concat(hourValueSpan, minuteSpanBuilder);
        timeView.setText(finalText);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setEventItem(EventItem eventItem) {
        Bundle arg = new Bundle();
        arg.putParcelable(ARG_EVENT_ITEM, eventItem);
        setArguments(arg);
    }

    @Override
    public void onResume() {
        if (mHasMap) {
            mMapView.onResume();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHasMap) {
            mMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mHasMap) {
            mMapView.onLowMemory();
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = null;
        switch (view.getId()) {
            case R.id.phone_button:
                intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + mEventItem.phone));
                getActivity().startActivity(intent);
                break;

            case R.id.email_button:

                intent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:?to=" + mEventItem.email);
                intent.setData(data);
                startActivity(intent);
                break;

            default:
        }
    }
}
