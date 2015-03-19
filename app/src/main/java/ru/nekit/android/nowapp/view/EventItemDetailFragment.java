package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.utils.TextViewUtils;
import ru.nekit.android.nowapp.view.textDecoration.SuperscriptSpanAdjuster;

@SuppressWarnings("ResourceType")
public class EventItemDetailFragment extends Fragment {

    public static final String TAG = "ru.nekit.android.event_item_detail_fragment";

    private static final String ARG_EVENT_ITEM = "ru.nekit.android.event_item";

    public EventItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arg = getArguments();
        Context context = getActivity();
        EventItem eventItem = arg.getParcelable(ARG_EVENT_ITEM);
        View view = inflater.inflate(R.layout.fragment_event_item_detail, container, false);
        TextView titleView = (TextView) view.findViewById(R.id.title_view);
        titleView.setText(eventItem.name);
        Glide.with(getActivity()).load(eventItem.posterThumb).into((ImageView) view.findViewById(R.id.poster_thumb_view));
        String logoThumb = eventItem.logoThumb;
        ImageView logoThumbView = (ImageView) view.findViewById(R.id.logo_view);
        TextView placeView = (TextView) view.findViewById(R.id.place_view);
        if ("".equals(logoThumb)) {
            logoThumbView.setVisibility(View.GONE);
            placeView.setVisibility(View.VISIBLE);
            placeView.setText(eventItem.placeName);
        } else {
            logoThumbView.setVisibility(View.VISIBLE);
            placeView.setVisibility(View.GONE);
            Glide.with(context).load(eventItem.logoThumb).into(logoThumbView);
        }
        int categoryDrawableId = EventItemsModel.getCategoryBigDrawable(eventItem.category);
        if (categoryDrawableId != 0) {
            ((ImageView) view.findViewById(R.id.category_type_view)).setImageDrawable(getActivity().getResources().getDrawable(categoryDrawableId));
        }

        TextView dayDateView = (TextView) view.findViewById(R.id.day_date_view);
        TextView monthView = (TextView) view.findViewById(R.id.month_view);
        TextView dayOfWeekView = (TextView) view.findViewById(R.id.day_of_week_view);
        TextView timeView = (TextView) view.findViewById(R.id.time_view);
        TextView entranceView = (TextView) view.findViewById(R.id.entrance_view);

        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(eventItem.date * 1000);
        dayDateView.setText(String.format("%d", cl.get(Calendar.DAY_OF_MONTH)));
        monthView.setText(new DateFormatSymbols().getMonths()[cl.get(Calendar.MONTH)]);
        dayOfWeekView.setText(getResources().getTextArray(R.array.day_of_week)[cl.get(Calendar.DAY_OF_WEEK)]);
        createEventTimeTextBlock(context, timeView, cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE));

        return view;
    }


    private void createEventTimeTextBlock(Context context, TextView timeView, int hour, int minute) {
        String hourTextValue = String.format("%d", hour);
        String minuteTextValue = String.format("%d", minute);
        if(hour < 9){
            hourTextValue = "0".concat(hourTextValue);
        }
        if(minute < 9){
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

        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        int baseHeight = TextViewUtils.getTextBounds(hourTextValue, hourTextSize - 2 * scaledDensity, Typeface.DEFAULT).height();
        SpannableString hourValueSpan = new SpannableString(hourTextValue);
        hourValueSpan.setSpan(new TextAppearanceSpan(context, R.style.HourTextStyle), 0, hourTextValue.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString minuteValueSpan = new SpannableString(minuteTextValue);
        minuteValueSpan.setSpan(new SuperscriptSpanAdjuster(context, R.style.MinuteTextStyle, baseHeight), 0, minuteTextValue.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence finalText = TextUtils.concat(hourValueSpan, minuteValueSpan);
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
}
