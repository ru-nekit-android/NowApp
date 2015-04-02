package ru.nekit.android.nowapp.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.R;

/**
 * Created by chuvac on 15.03.15.
 */
public class EventItemsModel {

    public static final String TYPE = "type";

    public static final String REQUEST_NEW_EVENT_ITEMS = "request_new_event_items";
    public static final String REFRESH_EVENT_ITEMS = "refresh_event_items";

    private static final HashMap<String, Integer> CATEGORY_TYPE = new HashMap();
    private static final HashMap<String, Integer> CATEGORY_TYPE_BIG = new HashMap();

    private static final EventItemsModel instance;

    private ArrayList<EventItem> mEventItemsList;
    private int mAvailableEventCount;

    static {
        instance = new EventItemsModel();
    }

    private EventItemsModel() {
        mEventItemsList = new ArrayList<>();
        CATEGORY_TYPE.put("category_sport", R.drawable.cat_sport);
        CATEGORY_TYPE.put("category_entertainment", R.drawable.cat_drink);
        CATEGORY_TYPE.put("category_other", R.drawable.cat_crown);
        CATEGORY_TYPE.put("category_education", R.drawable.cat_book);
        CATEGORY_TYPE_BIG.put("category_sport", R.drawable.cat_sport_big);
        CATEGORY_TYPE_BIG.put("category_entertainment", R.drawable.cat_drink_big);
        CATEGORY_TYPE_BIG.put("category_other", R.drawable.cat_crown_big);
        CATEGORY_TYPE_BIG.put("category_education", R.drawable.cat_book_big);
    }

    public static int getCategoryDrawable(String category) {
        return CATEGORY_TYPE.get(category);

    }

    public static int getCategoryBigDrawable(String category) {
        return CATEGORY_TYPE_BIG.get(category);
    }

    public static EventItemsModel getInstance() {
        return instance;
    }

    public void addEvents(ArrayList<EventItem> eventItems) {
        if (isEventItemsListEmpty()) {
            mEventItemsList.addAll(eventItems);
        } else {
            for (int i = 0; i < eventItems.size(); i++) {
                EventItem eventItem = eventItems.get(i);
                mEventItemsList.add(eventItem);
            }
        }
    }

    public void setEvents(ArrayList<EventItem> eventList) {
        mEventItemsList.clear();
        mEventItemsList.addAll(eventList);
    }

    public EventItem getLastEvent() {
        return mEventItemsList.size() > 0 ? mEventItemsList.get(mEventItemsList.size() - 1) : null;
    }

    public ArrayList<EventItem> getEventItemsList() {
        return mEventItemsList;
    }

    /*public int getIndexWithId(int id) {
        for (int i = 0; i < mEventItemsList.size(); i++) {
            if (mEventItemsList.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }*/

    public static long getCurrentTimestamp(Context context, boolean usePrecision) {
        Calendar calendar = Calendar.getInstance();
        int minutes = calendar.get(Calendar.MINUTE);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        long currentTimeTimestamp = TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(minutes);
        if (usePrecision) {
            long precision = TimeUnit.MINUTES.toSeconds(context.getResources().getInteger(R.integer.event_time_precision_in_minutes));
            currentTimeTimestamp = ((int) currentTimeTimestamp / precision) * precision;
        }
        return currentTimeTimestamp;
    }

    public static long getCurrentDateTimestamp(Context context, boolean usePrecision) {
        Calendar calendar = Calendar.getInstance();
        long currentDateTimestamp = TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis()) - getCurrentTimestamp(context, false);
        if (usePrecision) {
            long precision = TimeUnit.MINUTES.toSeconds(context.getResources().getInteger(R.integer.event_time_precision_in_minutes));
            currentDateTimestamp = ((int) currentDateTimestamp / precision) * precision;
        }
        return currentDateTimestamp;
    }

    public void setAvailableEventCount(int count) {
        mAvailableEventCount = count;
    }

    public boolean isEventItemsListEmpty() {
        return mEventItemsList.size() == 0;
    }

    public boolean isAvailableLoad() {
        return mEventItemsList.size() < mAvailableEventCount;
    }
}
