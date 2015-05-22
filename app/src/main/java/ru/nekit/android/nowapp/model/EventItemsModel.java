package ru.nekit.android.nowapp.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.VTAG;
import ru.nekit.android.nowapp.model.db.EventLocalDataSource;

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

    public static final int RESULT_OK = 0;
    public static final int DATA_IS_EMPTY = 1;

    private static final String TAG_EVENTS = "events";
    private static final String SITE_NAME = "nowapp.ru";
    private static final String API_ROOT = "api/events.get";
    private static final String DATABASE_NAME = "nowapp.db";
    private static final int DATABASE_VERSION = 3;

    public static final String LOAD_IN_BACKGROUND_NOTIFICATION = "ru.nekit.abdroid.nowapp.load_in_bavkgroun_result";
    public static final String LOADING_TYPE = "loading_type";
    public static final String REQUEST_NEW_EVENTS = "request_new_event_items";
    public static final String REFRESH_EVENTS = "refresh_event_items";
    public static final String LOAD_IN_BACKGROUND = "load_in_background";

    private static final boolean FEATURE_LOAD_IN_BACKGROUND = true;

    private static final HashMap<String, String> CATEGORY_TYPE_KEYWORDS = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_COLOR = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_BIG = new HashMap<>();

    private static EventItemsModel sInstance;

    private final Context mContext;
    private final ArrayList<EventItem> mEventItems;
    private final EventLocalDataSource mEventLocalDataSource;
    private final Runnable mBackgroundLoadTask;

    private int mAvailableEventCount;
    private int mCurrentPage;
    private boolean mReachEndOfDataList;

    private boolean mDataIsActual;
    private int mLoadedInBackgroundPage;
    private int mEventsCountPerPage;
    private EventItem mLastAddedInBackgroundEventItem;
    private Thread mBackgroundThread;

    private EventItemsModel(final Context context) {
        mContext = context;
        mEventItems = new ArrayList<>();
        mCurrentPage = 1;
        mLoadedInBackgroundPage = 1;
        mEventsCountPerPage = 0;
        mReachEndOfDataList = false;

        CATEGORY_TYPE.put("category_sport", R.drawable.category_sport);
        CATEGORY_TYPE.put("category_entertainment", R.drawable.category_entertainment);
        CATEGORY_TYPE.put("category_other", R.drawable.category_other);
        CATEGORY_TYPE.put("category_education", R.drawable.category_education);

        CATEGORY_TYPE_KEYWORDS.put("category_sport", context.getResources().getString(R.string.event_category_sport_keywords));
        CATEGORY_TYPE_KEYWORDS.put("category_entertainment", context.getResources().getString(R.string.event_category_entertainment_keywords));
        CATEGORY_TYPE_KEYWORDS.put("category_other", context.getResources().getString(R.string.event_category_other_keywords));
        CATEGORY_TYPE_KEYWORDS.put("category_education", context.getResources().getString(R.string.event_category_education_keywords));

        CATEGORY_TYPE_BIG.put("category_sport", R.drawable.event_category_sport_big);
        CATEGORY_TYPE_BIG.put("category_entertainment", R.drawable.event_category_entertainment_big);
        CATEGORY_TYPE_BIG.put("category_other", R.drawable.event_category_other_big);
        CATEGORY_TYPE_BIG.put("category_education", R.drawable.event_category_education_big);
        CATEGORY_TYPE_COLOR.put("category_sport", context.getResources().getColor(R.color.event_category_sport));
        CATEGORY_TYPE_COLOR.put("category_entertainment", context.getResources().getColor(R.color.event_category_entertainment));
        CATEGORY_TYPE_COLOR.put("category_other", context.getResources().getColor(R.color.event_category_other));
        CATEGORY_TYPE_COLOR.put("category_education", context.getResources().getColor(R.color.event_category_education));

        mEventLocalDataSource = new EventLocalDataSource(context, DATABASE_NAME, DATABASE_VERSION);

        mBackgroundLoadTask = new Runnable() {
            @Override
            public void run() {
                Bundle args = new Bundle();
                args.putString(LOADING_TYPE, LOAD_IN_BACKGROUND);
                int result = RESULT_OK;
                while (!Thread.interrupted() && mLoadedInBackgroundPage < getAvailablePageCount() && result == RESULT_OK) {
                    result = performLoad(context, args);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOAD_IN_BACKGROUND_NOTIFICATION));
                    VTAG.call("Background Load Task: " + mLoadedInBackgroundPage + " : " + getAvailablePageCount() + " : " + result);
                }
            }
        };

        mEventLocalDataSource.openForWrite();


        ArrayList<EventItem> allEvents = mEventLocalDataSource.getAllEvents();
        for (EventItem event : allEvents) {
            if (!eventIsActual(event)) {
                mEventLocalDataSource.removeEventByID(event.id);
            }
        }

        setEventsFromLocalDataSource(allEvents);
    }

    public void registerForLoadInBackgroundResultNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, new IntentFilter(LOAD_IN_BACKGROUND_NOTIFICATION));
    }

    public void unregisterForLoadInBackgroundResultNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }

    private boolean eventIsActual(EventItem eventItem) {
        return (eventItem.date + eventItem.endAt) > (getCurrentTimeTimestamp(mContext, true) + getCurrentDateTimestamp(mContext, true));
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

    //TODO: get from database!!!
    public EventItem getEventItemByID(int ID) {
        EventItem result = null;
        for (int i = 0; i < mEventItems.size() && result == null; i++) {
            EventItem item = mEventItems.get(i);
            if (item.id == ID) {
                result = item;
            }
        }
        return result;
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

    public static String getCategoryKeywords(String category) {
        return CATEGORY_TYPE_KEYWORDS.get(category);
    }

    public static synchronized EventItemsModel getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new EventItemsModel(context);
        }
        return sInstance;
    }

    public void addEvents(ArrayList<EventItem> eventItems) {
        mEventItems.addAll(eventItems);
    }

    public void setEvents(ArrayList<EventItem> eventItems) {
        mEventItems.clear();
        mEventItems.addAll(eventItems);
    }

    public ArrayList<EventItem> getEventItems() {
        return mEventItems;
    }

    public boolean dataIsActual() {
        return mDataIsActual;
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

    public boolean isAvailableLoad() {
        return mEventItems.size() < mAvailableEventCount && !mReachEndOfDataList;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }


    private ArrayList<EventItem> sortByStartTime(ArrayList<EventItem> eventItems) {
        Collections.sort(eventItems, new EventNameComparator());
        return eventItems;
    }

    private void setEventsFromLocalDataSource(ArrayList<EventItem> allEvents) {
        mDataIsActual = false;
        VTAG.call("set Events From Data Source: " + allEvents.size());
        setEvents(sortByStartTime(allEvents));
    }

    private void insertEventToLocalDataSource(EventItem eventItem) {
        mEventLocalDataSource.createOrUpdateEvent(eventItem);
    }


    private void loadInBackground() {
        if (FEATURE_LOAD_IN_BACKGROUND) {
            if (mBackgroundThread != null && mBackgroundThread.isAlive()) {
                mBackgroundThread.interrupt();
            }
            mBackgroundThread = new Thread(mBackgroundLoadTask);
            mBackgroundThread.start();
        }
    }

    private int getAvailablePageCount() {
        return mAvailableEventCount / mEventsCountPerPage + (mAvailableEventCount % mEventsCountPerPage == 0 ? 0 : 1);
    }

    private class EventNameComparator implements Comparator<EventItem> {
        public int compare(EventItem left, EventItem right) {
            return Long.valueOf(left.date + left.startAt).compareTo(right.date + right.startAt);
        }
    }

    ArrayList<EventItem> performSearch(String query) {
        String[] splitQuery = mEventLocalDataSource.normalizeForSearch(query).split(" ");
        ArrayList<String> queryList = new ArrayList<>();
        for (String item : splitQuery) {
            queryList.add(item + "*");
        }
        String queryResult = TextUtils.join(" ", queryList);
        return mEventLocalDataSource.fullTextSearch(queryResult);
    }

    int performLoad(Context context, Bundle args) {

        Integer result = RESULT_OK;

        String type = null;

        if (args != null) {
            type = args.getString(LOADING_TYPE);
        }

        boolean requestNewEvents = REQUEST_NEW_EVENTS.equals(type);

        if (FEATURE_LOAD_IN_BACKGROUND) {
            if (requestNewEvents) {
                if (mCurrentPage < mLoadedInBackgroundPage) {
                    if (mCurrentPage <= getAvailablePageCount()) {
                        mCurrentPage = mLoadedInBackgroundPage;
                        setEvents(sortByStartTime(mEventLocalDataSource.getAllEvents()));
                        return result;
                    }
                } else {
                    mReachEndOfDataList = true;
                }
            }
        }

        boolean loadInBackground = LOAD_IN_BACKGROUND.equals(type);
        boolean refreshEvents = REFRESH_EVENTS.equals(type) || type == null;

        ArrayList<EventItem> eventList = new ArrayList<>();

        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            Uri.Builder uriBuilder = new Uri.Builder()
                    .scheme("http")
                    .authority(SITE_NAME)
                    .path(API_ROOT);
            if (requestNewEvents || loadInBackground) {
                EventItem lastEventItem = requestNewEvents ? mEventItems.get(mEventItems.size() - 1) : mLastAddedInBackgroundEventItem;
                if (lastEventItem != null) {
                    String lastEventItemId = String.format("%d", lastEventItem.id);
                    uriBuilder
                            .appendQueryParameter("fl", "0")
                            .appendQueryParameter("date", String.format("%d", lastEventItem.date))
                            .appendQueryParameter("startAt", String.format("%d", lastEventItem.startAt))
                            .appendQueryParameter("id", lastEventItemId);
                }
            } else {
                uriBuilder
                        .appendQueryParameter("fl", "1")
                        .appendQueryParameter("date", String.format("%d", getCurrentDateTimestamp(context, true)))
                        .appendQueryParameter("startAt", String.format("%d", getCurrentTimeTimestamp(context, true)));
            }

            Uri uri = uriBuilder.build();
            String query = uri.toString();

            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(query);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                String jsonString = EntityUtils.toString(httpEntity);

                try {
                    JSONObject jsonRootObject = new JSONObject(jsonString);

                    String response = jsonRootObject.getString("response");
                    mAvailableEventCount = jsonRootObject.getInt("events_count");

                    if ("ok".equals(response)) {
                        JSONArray eventJsonArray = jsonRootObject.getJSONArray(TAG_EVENTS);
                        for (int i = 0; i < eventJsonArray.length(); i++) {
                            JSONObject jsonEventItem = eventJsonArray.getJSONObject(i);

                            EventItem eventItem = new EventItem();
                            eventItem.date = jsonEventItem.optLong(EventFieldNameDictionary.DATE, 0);
                            eventItem.eventDescription = jsonEventItem.optString(EventFieldNameDictionary.EVENT_DESCRIPTION);
                            eventItem.placeName = jsonEventItem.optString(EventFieldNameDictionary.PLACE_NAME);
                            eventItem.placeId = jsonEventItem.optInt(EventFieldNameDictionary.PLACE_ID);
                            eventItem.id = jsonEventItem.optInt(EventFieldNameDictionary.ID);
                            eventItem.category = jsonEventItem.optString(EventFieldNameDictionary.EVENT_CATEGORY);
                            eventItem.entrance = jsonEventItem.optString(EventFieldNameDictionary.ENTRANCE);
                            eventItem.address = jsonEventItem.optString(EventFieldNameDictionary.ADDRESS);
                            eventItem.phone = jsonEventItem.optString(EventFieldNameDictionary.PHONE);
                            eventItem.site = jsonEventItem.optString(EventFieldNameDictionary.SITE);
                            eventItem.email = jsonEventItem.optString(EventFieldNameDictionary.EMAIL);
                            eventItem.lat = jsonEventItem.optDouble(EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE);
                            eventItem.lng = jsonEventItem.optDouble(EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE);
                            eventItem.name = jsonEventItem.optString(EventFieldNameDictionary.NAME);
                            eventItem.startAt = jsonEventItem.optLong(EventFieldNameDictionary.START_AT);
                            eventItem.endAt = jsonEventItem.optLong(EventFieldNameDictionary.END_AT);
                            eventItem.posterThumb = jsonEventItem.optString(EventFieldNameDictionary.POSTER_THUMB);
                            eventItem.posterBlur = jsonEventItem.optString(EventFieldNameDictionary.POSTER_BLUR);
                            eventItem.posterOriginal = jsonEventItem.optString(EventFieldNameDictionary.POSTER_ORIGINAL);
                            eventItem.logoOriginal = jsonEventItem.optString(EventFieldNameDictionary.LOGO_ORIGINAL);
                            eventItem.logoThumb = jsonEventItem.optString(EventFieldNameDictionary.LOGO_THUMB);
                            eventItem.allNightParty = jsonEventItem.optBoolean(EventFieldNameDictionary.ALL_NIGHT_PARTY) ? 1 : 0;

                            eventList.add(eventItem);

                        }
                    } else {
                        //error
                    }
                } catch (JSONException exp) {
                    result = -1;
                    exp.printStackTrace();
                }
            } catch (UnsupportedEncodingException exp) {
                result = -1;
                exp.printStackTrace();
            } catch (ClientProtocolException exp) {
                result = -1;
                exp.printStackTrace();
            } catch (IOException exp) {
                result = -1;
                exp.printStackTrace();
            }
            if (eventList.size() > 0) {
                if (requestNewEvents) {
                    addEvents(eventList);
                    mCurrentPage++;
                } else if (refreshEvents) {
                    setEvents(eventList);
                    mReachEndOfDataList = false;
                    mCurrentPage = 1;
                    mLoadedInBackgroundPage = 1;
                } else if (loadInBackground) {
                    mLoadedInBackgroundPage++;
                }
                if (refreshEvents || loadInBackground) {
                    mLastAddedInBackgroundEventItem = eventList.get(eventList.size() - 1);
                    if (refreshEvents && mEventsCountPerPage == 0) {
                        mEventsCountPerPage = eventList.size();
                    }
                    if (mLoadedInBackgroundPage == 1) {
                        mEventLocalDataSource.clear();
                        loadInBackground();
                    }
                    for (int i = 0; i < eventList.size(); i++) {
                        insertEventToLocalDataSource(eventList.get(i));
                    }
                    NowApplication.updateDataTimestamp();
                }
                mDataIsActual = true;
            } else {
                mReachEndOfDataList = !loadInBackground;
                result = DATA_IS_EMPTY;
            }
        } else {
            setEventsFromLocalDataSource(mEventLocalDataSource.getAllEvents());
            mAvailableEventCount = getEventItems().size();
        }

        return result;
    }
}
