package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.support.v4.util.Pair;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.db.EventLocalDataSource;
import ru.nekit.android.nowapp.model.db.EventToCalendarLinker;
import ru.nekit.android.nowapp.model.db.vo.EventToCalendarLink;

/**
 * Created by chuvac on 15.03.15.
 */
public class EventItemsModel {


    private static final long MAXIMUM_TIME_PERIOD_FOR_DATE_ALIAS_SUPPORT = TimeUnit.HOURS.toSeconds(4);

    private static final long[] NIGHT_PERIOD = {TimeUnit.HOURS.toSeconds(23), TimeUnit.HOURS.toSeconds(5) - 1};
    private static final long[] MORNING_PERIOD = {TimeUnit.HOURS.toSeconds(5), TimeUnit.HOURS.toSeconds(12) - 1};
    private static final long[] DAY_PERIOD = {TimeUnit.HOURS.toSeconds(12), TimeUnit.HOURS.toSeconds(19) - 1};
    private static final long[] EVENING_PERIOD = {TimeUnit.HOURS.toSeconds(19), TimeUnit.HOURS.toSeconds(23) - 1};

    private static ArrayList<Pair<String, long[]>> PERIODS = new ArrayList<Pair<String, long[]>>(4) {{
        add(new Pair<>("ночью", NIGHT_PERIOD));
        add(new Pair<>("утром", MORNING_PERIOD));
        add(new Pair<>("днем", DAY_PERIOD));
        add(new Pair<>("вечером", EVENING_PERIOD));
    }};

    private static final String LOCAL_DATABASE_NAME = "nowapp.db";

    public static final String LOADING_TYPE = "loading_type";
    public static final String REQUEST_NEW_EVENT_ITEMS = "request_new_event_items";
    public static final String REFRESH_EVENT_ITEMS = "refresh_event_items";

    private static final HashMap<String, Integer> CATEGORY_TYPE = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_COLOR = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_BIG = new HashMap<>();

    private static EventItemsModel instance;

    private final ArrayList<EventItem> mEventItems;
    private final ArrayList<EventItem> mLastAddedEventItems;
    private final EventToCalendarLinker mEventToCalendarLinker;
    private final Context mContext;
    private int mAvailableEventCount;
    private int mCurrentPage;
    private boolean mReachEndOfDataList;
    private EventLocalDataSource mEventLocalDataSource;

    private EventItemsModel(Context context) {
        mContext = context;
        mEventItems = new ArrayList<>();
        mLastAddedEventItems = new ArrayList<>();
        mCurrentPage = 1;
        mReachEndOfDataList = false;
        CATEGORY_TYPE.put("category_sport", R.drawable.category_sport);
        CATEGORY_TYPE.put("category_entertainment", R.drawable.category_entertainment);
        CATEGORY_TYPE.put("category_other", R.drawable.category_other);
        CATEGORY_TYPE.put("category_education", R.drawable.category_education);
        CATEGORY_TYPE_BIG.put("category_sport", R.drawable.category_sport_big);
        CATEGORY_TYPE_BIG.put("category_entertainment", R.drawable.category_entertainment_big);
        CATEGORY_TYPE_BIG.put("category_other", R.drawable.category_other_big);
        CATEGORY_TYPE_BIG.put("category_education", R.drawable.category_education_big);
        CATEGORY_TYPE_COLOR.put("category_sport", context.getResources().getColor(R.color.category_sport));
        CATEGORY_TYPE_COLOR.put("category_entertainment", context.getResources().getColor(R.color.category_entertainment));
        CATEGORY_TYPE_COLOR.put("category_other", context.getResources().getColor(R.color.category_other));
        CATEGORY_TYPE_COLOR.put("category_education", context.getResources().getColor(R.color.category_education));
        mEventLocalDataSource = new EventLocalDataSource(context, LOCAL_DATABASE_NAME);
        mEventToCalendarLinker = new EventToCalendarLinker(context, LOCAL_DATABASE_NAME);
    }

    private boolean eventIsActual(EventItem eventItem) {
        long currentTimeTimestamp = getCurrentTimeTimestamp(mContext, true);
        long startAfterSeconds = eventItem.startAt - currentTimeTimestamp;
        return startAfterSeconds >= 0 && ((eventItem.date + eventItem.endAt) > (currentTimeTimestamp + getCurrentDateTimestamp(mContext, true)));
    }

    public static String getStartTimeAlias(Context context, EventItem eventItem) {
        long currentTimeTimestamp = getCurrentTimeTimestamp(context, true);
        long startAfterSeconds = eventItem.startAt - currentTimeTimestamp;
        long dateDelta = eventItem.date - getCurrentDateTimestamp(context, true);
        String startTimeAliasString;
        startAfterSeconds += dateDelta;
        if (startAfterSeconds <= 0) {
            if (eventItem.endAt > currentTimeTimestamp) {
                startTimeAliasString = context.getResources().getString(R.string.going_right_now);
            } else {
                startTimeAliasString = context.getResources().getString(R.string.already_ended);
            }
        } else if (startAfterSeconds <= MAXIMUM_TIME_PERIOD_FOR_DATE_ALIAS_SUPPORT) {
            long startAfterMinutesFull = TimeUnit.SECONDS.toMinutes(startAfterSeconds);
            long startAfterHours = TimeUnit.MINUTES.toHours(startAfterMinutesFull);
            long startAfterMinutes = startAfterMinutesFull % TimeUnit.HOURS.toMinutes(1);
            startTimeAliasString = context.getResources().getString(R.string.going_in);
            if (startAfterHours > 0) {
                startTimeAliasString += String.format(" %d ч", startAfterHours);
            }
            if (startAfterMinutes > 0) {
                startTimeAliasString += String.format(" %d мин", startAfterMinutes);
            }
        } else {
            if (currentTimeTimestamp <= NIGHT_PERIOD[1] && eventItem.startAt >= MORNING_PERIOD[0]) {
                dateDelta += TimeUnit.DAYS.toSeconds(1);
            }
            if (dateDelta == 0) {
                startTimeAliasString = String.format("%s %s", context.getResources().getString(R.string.today), getDayPeriodAlias(eventItem.startAt));
            } else if (dateDelta >= TimeUnit.DAYS.toSeconds(1) && dateDelta < TimeUnit.DAYS.toSeconds(2)) {
                startTimeAliasString = String.format("%s %s", context.getResources().getString(R.string.tomorrow), getDayPeriodAlias(eventItem.startAt));
            } else if (dateDelta >= TimeUnit.DAYS.toSeconds(2) && dateDelta < TimeUnit.DAYS.toSeconds(3)) {
                startTimeAliasString = context.getResources().getString(R.string.day_after_tomorrow);
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(eventItem.date));
                startTimeAliasString = String.format("%s %s", calendar.get(Calendar.DAY_OF_MONTH), new DateFormatSymbols().getMonths()[calendar.get(Calendar.MONTH)].toLowerCase());
            }
        }


        return startTimeAliasString;
    }

    private static String getDayPeriodAlias(long dayTimeInSeconds) {
        String alias = PERIODS.get(0).first;
        for (int i = 1; i < PERIODS.size(); i++) {
            long[] periodTime = PERIODS.get(i).second;
            if (dayTimeInSeconds >= periodTime[0] && dayTimeInSeconds <= periodTime[1]) {
                alias = PERIODS.get(i).first;
            }
        }
        return alias;
    }

    public static int getCategoryColor(String category) {
        return CATEGORY_TYPE_COLOR.get(category);
    }

    public static int getCategoryDrawable(String category) {
        return CATEGORY_TYPE.get(category);
    }

    public static int getCategoryBigDrawable(String category) {
        return CATEGORY_TYPE_BIG.get(category);
    }

    public static EventItemsModel getInstance(Context context) {
        if (instance == null) {
            instance = new EventItemsModel(context);
        }
        return instance;
    }

    public void addEvents(ArrayList<EventItem> eventItems) {
        mLastAddedEventItems.clear();
        mLastAddedEventItems.addAll(eventItems);
        mEventItems.addAll(eventItems);
    }

    public void setEvents(ArrayList<EventItem> eventItems) {
        mEventItems.clear();
        mEventItems.addAll(eventItems);
    }

    public EventItem getLastEvent() {
        return mEventItems.size() > 0 ? mEventItems.get(mEventItems.size() - 1) : null;
    }

    public ArrayList<EventItem> getEventItems() {
        return mEventItems;
    }

    public ArrayList<EventItem> getLastAddedEventItems() {
        return mLastAddedEventItems;
    }

    public static long getCurrentTimeTimestamp(Context context, boolean usePrecision) {
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
        long currentDateTimestamp = TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis()) - getCurrentTimeTimestamp(context, false);
        if (usePrecision) {
            long precision = TimeUnit.MINUTES.toSeconds(context.getResources().getInteger(R.integer.event_time_precision_in_minutes));
            currentDateTimestamp = ((int) currentDateTimestamp / precision) * precision;
        }
        return currentDateTimestamp + getTimeZoneOffsetInSeconds();
    }

    private static long getTimeZoneOffsetInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTimeZone().getRawOffset());
    }

    public static long getEventStartTimeInSeconds(EventItem eventItem) {
        return eventItem.date + eventItem.startAt - getTimeZoneOffsetInSeconds();
    }

    public static long getEventEndTimeInSeconds(EventItem eventItem) {
        return eventItem.date + eventItem.endAt - getTimeZoneOffsetInSeconds();
    }

    public void setAvailableEventCount(int count) {
        mAvailableEventCount = count;
    }

    public boolean isEventItemsListEmpty() {
        return mEventItems.size() == 0;
    }

    public boolean isAvailableLoad() {
        return mEventItems.size() < mAvailableEventCount && !mReachEndOfDataList;
    }

    public void incrementCurrentPage() {
        mCurrentPage++;
    }

    public void setDefaultCurrentPage() {
        mCurrentPage = 1;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setReachEndOfDataList(boolean value) {
        mReachEndOfDataList = value;
    }

    public EventLocalDataSource getLocalDataSource() {
        return mEventLocalDataSource;
    }

    public void sortByStartTime() {
        Collections.sort(mEventItems, new EventNameComparator());
    }

    public EventToCalendarLinker getEventToCalendarLinker() {
        return mEventToCalendarLinker;
    }

    public void removeIrrelevantEvents() {
        mEventLocalDataSource.openForWrite();
        mEventToCalendarLinker.openForWrite();
        ArrayList<EventItem> allEvents = mEventLocalDataSource.getAllEvents();
        for (int i = 0; i < allEvents.size(); i++) {
            EventItem event = allEvents.get(i);
            if (!eventIsActual(event)) {
                mEventLocalDataSource.removeEventByID(event.id);
                mEventToCalendarLinker.removeLinkByEventID(event.id);
            }
        }
        mEventLocalDataSource.close();
        mEventToCalendarLinker.close();
    }

    public class EventNameComparator implements Comparator<EventItem> {
        public int compare(EventItem left, EventItem right) {
            return Long.valueOf(left.date + left.startAt).compareTo(right.date + right.startAt);
        }
    }

    public EventToCalendarLink addEventToCalendarLink(EventItem eventItem, long calendarEventID) {
        return mEventToCalendarLinker.addLink(eventItem.id, calendarEventID);
    }

    public void removeEventToCalendarLink(EventItem eventItem) {
        mEventToCalendarLinker.removeLinkByEventID(eventItem.id);
    }

    public EventToCalendarLink getEventToCalendarLink(EventItem eventItem) {
        return mEventToCalendarLinker.getLinkByEventID(eventItem.id);
    }
}
