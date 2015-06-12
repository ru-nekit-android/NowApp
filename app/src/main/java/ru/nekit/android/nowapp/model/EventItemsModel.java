package ru.nekit.android.nowapp.model;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.VTAG;
import ru.nekit.android.nowapp.model.db.EventLocalDataSource;
import ru.nekit.android.nowapp.model.db.EventStatsLocalDataSource;
import ru.nekit.android.nowapp.model.db.EventToCalendarDataSource;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;

/**
 * Created by chuvac on 15.03.15.
 */
public class EventItemsModel {

    public static final int RESULT_OK = 0;
    public static final int RESULT_BAD = -1;
    public static final int LIKED_CONFIRMED = 2;
    public static final int LIKED_NOT_CONFIRMED = 1;
    public static final int DATA_IS_EMPTY = 1;
    public static final String LOAD_IN_BACKGROUND_NOTIFICATION = "ru.nekit.android.nowapp.load_in_background_result";
    public static final String LOADING_TYPE = "loading_type";
    public static final String REQUEST_NEW_EVENTS = "request_new_event_items";
    public static final String REFRESH_EVENTS = "refresh_event_items";
    public static final String LOAD_IN_BACKGROUND = "load_in_background";
    private static final long MAXIMUM_TIME_PERIOD_FOR_DATE_ALIAS_SUPPORT = TimeUnit.HOURS.toSeconds(4);
    private static final long[] NIGHT_PERIOD = {TimeUnit.HOURS.toSeconds(23), TimeUnit.HOURS.toSeconds(5) - 1};
    private static final long[] MORNING_PERIOD = {TimeUnit.HOURS.toSeconds(5), TimeUnit.HOURS.toSeconds(12) - 1};
    private static final long[] DAY_PERIOD = {TimeUnit.HOURS.toSeconds(12), TimeUnit.HOURS.toSeconds(19) - 1};
    private static final long[] EVENING_PERIOD = {TimeUnit.HOURS.toSeconds(19), TimeUnit.HOURS.toSeconds(23) - 1};
    private static final String TAG_EVENTS = "events";
    private static final String SITE_NAME = "nowapp.ru";
    private static final String API_ROOT = "api/event";
    private static final String API_REQUEST_GET_EVENTS = API_ROOT + "s.get";
    private static final String API_REQUEST_GET_STATS = API_ROOT + ".get.stats";
    private static final String API_REQUEST_UPDATE_LIKE = API_ROOT + ".update.like";
    private static final String API_REQUEST_UPDATE_VIEW = API_ROOT + ".update.view";

    private static final String DATABASE_NAME = "nowapp.db";
    private static final int DATABASE_VERSION = 8;

    private boolean FEATURE_LOAD_IN_BACKGROUND() {
        return mContext.getResources().getBoolean(R.bool.feature_load_in_background);
    }

    private int PAGE_LIMIT_LOAD_IN_BACKGROUND() {
        return mContext.getResources().getInteger(R.integer.page_limit_load_in_background);
    }

    private static final HashMap<String, String> CATEGORY_TYPE_KEYWORDS = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_COLOR = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_BIG = new HashMap<>();
    private static ArrayList<Pair<String, long[]>> PERIODS = new ArrayList<Pair<String, long[]>>(4) {{
        add(new Pair<>("ночью", NIGHT_PERIOD));
        add(new Pair<>("утром", MORNING_PERIOD));
        add(new Pair<>("днем", DAY_PERIOD));
        add(new Pair<>("вечером", EVENING_PERIOD));
    }};
    private static EventItemsModel sInstance;

    private final Context mContext;
    private final ArrayList<EventItem> mEventItems;
    private final EventLocalDataSource mEventLocalDataSource;
    private final EventStatsLocalDataSource mEventStatsLocalDataSource;

    private final Runnable mBackgroundLoadTask;
    private final EventToCalendarDataSource mEventToCalendarDataSource;

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
        mEventStatsLocalDataSource = new EventStatsLocalDataSource(context, DATABASE_NAME, DATABASE_VERSION);
        mEventToCalendarDataSource = new EventToCalendarDataSource(context, DATABASE_NAME, DATABASE_VERSION);

        mBackgroundLoadTask = new Runnable() {
            @Override
            public void run() {
                int result = RESULT_OK;
                while (!Thread.interrupted() && mLoadedInBackgroundPage < getAvailablePageCount() && mLoadedInBackgroundPage <= PAGE_LIMIT_LOAD_IN_BACKGROUND() && result == RESULT_OK) {
                    try {
                        result = performEventsLoad(LOAD_IN_BACKGROUND);
                    } catch (IOException | JSONException exp) {
                    }
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOAD_IN_BACKGROUND_NOTIFICATION));
                    VTAG.call("Background Load Task: " + mLoadedInBackgroundPage + " : " + getAvailablePageCount() + " : " + result);
                }
            }
        };

        mEventLocalDataSource.openForWrite();
        mEventStatsLocalDataSource.openForWrite();
        mEventToCalendarDataSource.openForWrite();

        ArrayList<EventItem> allEvents = mEventLocalDataSource.getAllEvents();
        for (EventItem event : allEvents) {
            if (!eventIsActual(event)) {
                mEventLocalDataSource.removeEventByID(event.id);
                mEventToCalendarDataSource.removeLinkByEventID(event.id);
            }
        }

        setEventsFromLocalDataSource(allEvents);
    }

    private EventToCalendarLink addEventToCalendarLink(EventItem eventItem, long calendarEventID) {
        return mEventToCalendarDataSource.addLink(eventItem.id, calendarEventID);
    }

    private void removeEventToCalendarLinkByEventId(long eventItemId) {
        mEventToCalendarDataSource.removeLinkByEventID(eventItemId);
    }

    private EventToCalendarLink getEventToCalendarLinkByEventId(long eventItemId) {
        return mEventToCalendarDataSource.getLinkByEventID(eventItemId);
    }


    public static String getStartTimeAlias(Context context, EventItem eventItem) {
        return getStartTimeString(context, eventItem, false);
    }

    public static String getStartTimeKeywords(Context context, EventItem eventItem) {
        return getStartTimeString(context, eventItem, true).toLowerCase();
    }

    private static String getStartTimeString(Context context, EventItem eventItem, boolean keywords) {
        long currentTimeTimestamp = getCurrentTimeTimestamp(context, true);
        long startAfterSeconds = eventItem.startAt - currentTimeTimestamp;
        long dateDelta = eventItem.date - getCurrentDateTimestamp(context, true);
        String startTimeAliasString;
        startAfterSeconds += dateDelta;
        if (startAfterSeconds <= 0) {
            if (eventItem.endAt > currentTimeTimestamp) {
                startTimeAliasString = context.getResources().getString(keywords ? R.string.going_right_now_keywords : R.string.going_right_now);
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

    public static String getCategoryKeywords(String category) {
        return CATEGORY_TYPE_KEYWORDS.get(category);
    }

    public static synchronized EventItemsModel getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new EventItemsModel(context);
        }
        return sInstance;
    }

    public static synchronized EventItemsModel getInstance() {
        assert sInstance != null;
        return sInstance;
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

    public void registerForLoadInBackgroundResultNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, new IntentFilter(LOAD_IN_BACKGROUND_NOTIFICATION));
    }

    public void unregisterForLoadInBackgroundResultNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }

    private boolean eventIsActual(EventItem eventItem) {
        return (eventItem.date + eventItem.endAt) > (getCurrentTimeTimestamp(mContext, true) + getCurrentDateTimestamp(mContext, true));
    }

    //TODO: get from database!!!
    private EventItem getEventItemByID(int ID) {
        EventItem result = null;
        for (int i = 0; i < mEventItems.size() && result == null; i++) {
            EventItem item = mEventItems.get(i);
            if (item.id == ID) {
                result = item;
            }
        }
        return result;
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

    private void loadInBackground() {
        if (FEATURE_LOAD_IN_BACKGROUND()) {
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

    ArrayList<EventItem> performSearch(String query) {
        String[] splitQuery = query.split(" ");
        ArrayList<String> queryList = new ArrayList<>();
        for (String item : splitQuery) {
            queryList.add(item + "*");
        }
        String queryResult = TextUtils.join(" ", queryList);
        return mEventLocalDataSource.fullTextSearch(queryResult);
    }

    int performGetStats(int eventId) throws IOException, JSONException {
        Integer result = RESULT_OK;
        EventItem eventItem = getEventItemByID(eventId);
        EventItemStats eventItemStats = getOrCreateEventItemStatsByEventId(eventId);
        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_STATS);
            uriBuilder.appendQueryParameter("id", Integer.toString(eventId));
            Uri uri = uriBuilder.build();
            String query = uri.toString();
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(query);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            String jsonString = EntityUtils.toString(httpEntity);
            JSONObject jsonRootObject = new JSONObject(jsonString);
            eventItemStats.likeCount = jsonRootObject.getInt(EventFieldNameDictionary.LIKE_COUNT);
            eventItemStats.viewCount = jsonRootObject.getInt(EventFieldNameDictionary.VIEW_COUNT);
        }
        mEventStatsLocalDataSource.createOrUpdateEventStats(eventItemStats);
        if (eventItem != null) {
            eventItem.stats = eventItemStats;
        }
        return result;
    }

    int performEventUpdateLike(int eventId) throws IOException, JSONException {
        ArrayList<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("key", "78GlLJhL"));
        EventItemStats eventItemStats = getOrCreateEventItemStatsByEventId(eventId);
        eventItemStats.myLikeStatus = LIKED_NOT_CONFIRMED;
        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            if (performPostApiCall(API_REQUEST_UPDATE_LIKE, eventId, parameters) == RESULT_OK) {
                eventItemStats.myLikeStatus = LIKED_CONFIRMED;
            }
        }
        mEventStatsLocalDataSource.createOrUpdateEventStats(eventItemStats);
        return RESULT_OK;
    }

    int performEventUpdateView(int eventId) throws IOException, JSONException {
        return performPostApiCall(API_REQUEST_UPDATE_VIEW, eventId, null);
    }

    private int performPostApiCall(String api, int eventId, @Nullable ArrayList<NameValuePair> parameters) throws IOException, JSONException {
        Integer result = RESULT_OK;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        ArrayList<NameValuePair> fullParameters = new ArrayList<>();
        fullParameters.add(new BasicNameValuePair("id", Integer.toString(eventId)));
        if (parameters != null && parameters.size() > 0) {
            fullParameters.addAll(parameters);
        }
        HttpPost httpPost = new HttpPost(getApiQuery(api));
        httpPost.setEntity(new UrlEncodedFormEntity(fullParameters));
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        String jsonString = EntityUtils.toString(httpEntity);
        JSONObject jsonRootObject = new JSONObject(jsonString);
        String response = jsonRootObject.getString("response");
        if (!("ok".equals(response))) {
            result = RESULT_BAD;
        }
        return result;
    }

    private Uri.Builder createApiUriBuilder(String apiMethod) {
        return new Uri.Builder()
                .scheme("http")
                .authority(SITE_NAME)
                .path(apiMethod);
    }

    private String getApiQuery(String apiMethod) {
        return createApiUriBuilder(apiMethod).build().toString();
    }

    int performEventsLoad(String loadingType) throws IOException, JSONException {

        Integer result = RESULT_OK;

        boolean requestNewEvents = REQUEST_NEW_EVENTS.equals(loadingType);

        if (FEATURE_LOAD_IN_BACKGROUND()) {
            if (requestNewEvents) {
                if (mLoadedInBackgroundPage > 1 && mCurrentPage <= mLoadedInBackgroundPage) {
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

        boolean loadInBackground = LOAD_IN_BACKGROUND.equals(loadingType);
        boolean refreshEvents = REFRESH_EVENTS.equals(loadingType) || loadingType == null;

        ArrayList<EventItem> eventList = new ArrayList<>();

        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_EVENTS);
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
                        .appendQueryParameter("date", String.format("%d", getCurrentDateTimestamp(mContext, true)))
                        .appendQueryParameter("startAt", String.format("%d", getCurrentTimeTimestamp(mContext, true)));
            }

            Uri uri = uriBuilder.build();
            String query = uri.toString();

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(query);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            String jsonString = EntityUtils.toString(httpEntity);

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
                            EventItem eventItem = eventList.get(i);
                            mEventLocalDataSource.createOrUpdateEvent(eventItem);
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
        }
        return result;
    }

    public EventToCalendarLink performCalendarFunctionality(int method, int eventItemId) {
        EventToCalendarLink result = null;
        long calendarEventID;
        ContentResolver contentResolver = mContext.getContentResolver();
        if (method == EventToCalendarLoader.CHECK) {
            result = getEventToCalendarLinkByEventId(eventItemId);
            if (result != null) {
                calendarEventID = result.getCalendarEventID();
                Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null, null, null);
                if (!cursor.moveToFirst()) {
                    result = null;
                    removeEventToCalendarLinkByEventId(eventItemId);
                }
                cursor.close();
            }
        } else if (method == EventToCalendarLoader.ADD) {
            EventItem eventItem = getEventItemByID(eventItemId);
            TimeZone timeZone = Calendar.getInstance().getTimeZone();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, TimeUnit.SECONDS.toMillis(getEventStartTimeInSeconds(eventItem)));
            values.put(CalendarContract.Events.DTEND, TimeUnit.SECONDS.toMillis(getEventEndTimeInSeconds(eventItem)));
            values.put(CalendarContract.Events.TITLE, eventItem.name);
            values.put(CalendarContract.Events.EVENT_LOCATION, eventItem.placeName);
            if (!"".equals(eventItem.email)) {
                values.put(CalendarContract.Events.ORGANIZER, eventItem.email);
            }
            values.put(CalendarContract.Events.DESCRIPTION, eventItem.eventDescription);
            values.put(CalendarContract.Events.EVENT_COLOR, getCategoryColor(eventItem.category));
            values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
            values.put(CalendarContract.Events.CALENDAR_ID, 1);
            Uri insertToCalendarURI = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
            calendarEventID = Long.parseLong(insertToCalendarURI.getLastPathSegment());
            result = addEventToCalendarLink(eventItem, calendarEventID);
        } else if (method == EventToCalendarLoader.REMOVE) {
            result = getEventToCalendarLinkByEventId(eventItemId);
            if (result != null) {
                calendarEventID = result.getCalendarEventID();
                Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null, null, null);
                removeEventToCalendarLinkByEventId(eventItemId);
                if (cursor.moveToFirst()) {
                    int status = contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null);
                    Assert.assertTrue(status != 0);
                }
                cursor.close();
                result = null;
            }
        }
        return result;
    }

    public EventItemStats getOrCreateEventItemStatsByEventId(int id) {
        EventItemStats eventItemStats = mEventStatsLocalDataSource.getByEventId(id);
        if (eventItemStats == null) {
            eventItemStats = new EventItemStats();
            eventItemStats.id = id;
            mEventStatsLocalDataSource.createOrUpdateEventStats(eventItemStats);
        }
        return eventItemStats;
    }

    private class EventNameComparator implements Comparator<EventItem> {
        public int compare(EventItem left, EventItem right) {
            return Long.valueOf(left.date + left.startAt).compareTo(right.date + right.startAt);
        }
    }
}
