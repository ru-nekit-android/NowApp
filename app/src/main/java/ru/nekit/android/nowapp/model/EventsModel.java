package ru.nekit.android.nowapp.model;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.VTAG;
import ru.nekit.android.nowapp.model.db.EventAdvertDataSource;
import ru.nekit.android.nowapp.model.db.EventDataSource;
import ru.nekit.android.nowapp.model.db.EventStatsDataSource;
import ru.nekit.android.nowapp.model.db.EventToCalendarDataSource;
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.model.vo.EventAdvert;
import ru.nekit.android.nowapp.model.vo.EventStats;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.OFFLINE;
import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

/**
 * Created by chuvac on 15.03.15.
 */
public class EventsModel {

    public static final int RESULT_OK = 0;
    public static final int RESULT_BAD = -1;
    public static final int LIKE_CONFIRMED = 2;
    public static final int LIKE_NOT_CONFIRMED = 1;
    public static final int DATA_IS_EMPTY = 1;
    public static final String LOADING_TYPE = "loading_type";
    public static final String REQUEST_NEW_EVENTS = "request_new_event_items";
    public static final String REFRESH_EVENTS = "refresh_event_items";
    public static final String LOAD_IN_BACKGROUND = "load_in_background";
    public static final String LOAD_IN_BACKGROUND_NOTIFICATION = "ru.nekit.android.nowapp.load_in_background_notification";
    public static final String UPDATE_FIVE_MINUTE_TIMER_NOTIFICATION = "ru.nekit.android.nowapp.update_five_minute_timer_notification";
    private static final long MAXIMUM_TIME_PERIOD_FOR_DATE_ALIAS_SUPPORT = TimeUnit.HOURS.toSeconds(4);
    private static final long[] NIGHT_PERIOD = {TimeUnit.HOURS.toSeconds(23), TimeUnit.HOURS.toSeconds(5) - 1};
    private static final long[] MORNING_PERIOD = {TimeUnit.HOURS.toSeconds(5), TimeUnit.HOURS.toSeconds(12) - 1};
    private static final long[] DAY_PERIOD = {TimeUnit.HOURS.toSeconds(12), TimeUnit.HOURS.toSeconds(19) - 1};
    private static final long[] EVENING_PERIOD = {TimeUnit.HOURS.toSeconds(19), TimeUnit.HOURS.toSeconds(23) - 1};
    private static final String TAG_EVENTS = "events";
    private static final String TAG_ADVERTS = "adverts";
    private static final String SITE_NAME = "nowapp.ru";
    private static final String API_ROOT = "api/event";
    private static final String API_REQUEST_GET_EVENTS = API_ROOT + "s.get";
    private static final String API_REQUEST_GET_EVENT = API_ROOT + ".get";
    private static final String API_REQUEST_GET_STATS = API_REQUEST_GET_EVENT + ".stats";
    private static final String API_REQUEST_UPDATE = API_ROOT + ".update";
    private static final String API_REQUEST_UPDATE_LIKE = API_REQUEST_UPDATE + ".like";
    private static final String API_REQUEST_UPDATE_VIEW = API_REQUEST_UPDATE + ".view";
    private static final String DATABASE_NAME = "nowapp.db";
    private static final int DATABASE_VERSION = 10;
    private static final HashMap<String, String> CATEGORY_TYPE_KEYWORDS = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE = new HashMap<>();
    private static final String CATEGORY_SPORT = "category_sport";
    private static final String CATEGORY_ENTERTAINMENT = "category_entertainment";
    private static final String CATEGORY_OTHER = "category_other";
    private static final String CATEGORY_EDUCATION = "category_education";
    private static final String CATEGORY_DISCOUNT = "category_discount";
    private static final String CATEGORY_MUSIC = "category_music";

    private static final HashMap<String, Integer> CATEGORY_TYPE_COLOR = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_BIG = new HashMap<>();
    @NonNull
    private static ArrayList<Pair<String, long[]>> PERIODS = new ArrayList<Pair<String, long[]>>(4) {{
        add(new Pair<>("ночью", NIGHT_PERIOD));
        add(new Pair<>("утром", MORNING_PERIOD));
        add(new Pair<>("днем", DAY_PERIOD));
        add(new Pair<>("вечером", EVENING_PERIOD));
    }};
    private static EventsModel sInstance;
    @NonNull
    private final Context mContext;
    @NonNull
    private final ArrayList<Event> mEvents;
    @NonNull
    private final EventDataSource mEventDataSource;
    @NonNull
    private final EventStatsDataSource mEventStatsDataSource;
    @NonNull
    private final EventAdvertDataSource mEventAdvertDataSource;
    @NonNull
    private final Runnable mBackgroundLoadTask;
    @NonNull
    private final EventToCalendarDataSource mEventToCalendarDataSource;
    @NonNull
    private final Timer mTimer;
    private int mAvailableEventCount;
    private int mCurrentPage;
    private boolean mReachEndOfDataList;
    private boolean mDataIsActual;
    private int mLoadedInBackgroundPage;
    private int mEventsCountPerPage;
    private Event mLastAddedInBackgroundEvent;
    private Thread mBackgroundThread;

    private EventsModel(@NonNull final Context context) {
        mContext = context;
        mEvents = new ArrayList<>();
        mCurrentPage = 1;
        mLoadedInBackgroundPage = 1;
        mEventsCountPerPage = 0;
        mReachEndOfDataList = false;

        CATEGORY_TYPE.put(CATEGORY_SPORT, R.drawable.event_category_sport);
        CATEGORY_TYPE.put(CATEGORY_ENTERTAINMENT, R.drawable.event_category_entertainment);
        CATEGORY_TYPE.put(CATEGORY_OTHER, R.drawable.event_category_other);
        CATEGORY_TYPE.put(CATEGORY_EDUCATION, R.drawable.event_category_education);
        CATEGORY_TYPE.put(CATEGORY_DISCOUNT, R.drawable.event_category_discount);
        CATEGORY_TYPE.put(CATEGORY_MUSIC, R.drawable.event_category_music);

        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_SPORT, context.getResources().getString(R.string.event_category_sport_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_ENTERTAINMENT, context.getResources().getString(R.string.event_category_entertainment_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_OTHER, context.getResources().getString(R.string.event_category_other_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_EDUCATION, context.getResources().getString(R.string.event_category_education_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_MUSIC, context.getResources().getString(R.string.event_category_music_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_DISCOUNT, context.getResources().getString(R.string.event_category_discount_keywords));

        CATEGORY_TYPE_BIG.put(CATEGORY_SPORT, R.drawable.event_category_sport_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_ENTERTAINMENT, R.drawable.event_category_entertainment_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_OTHER, R.drawable.event_category_other_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_EDUCATION, R.drawable.event_category_education_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_DISCOUNT, R.drawable.event_category_discount_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_MUSIC, R.drawable.event_category_music_big);

        CATEGORY_TYPE_COLOR.put(CATEGORY_SPORT, context.getResources().getColor(R.color.event_category_sport));
        CATEGORY_TYPE_COLOR.put(CATEGORY_ENTERTAINMENT, context.getResources().getColor(R.color.event_category_entertainment));
        CATEGORY_TYPE_COLOR.put(CATEGORY_OTHER, context.getResources().getColor(R.color.event_category_other));
        CATEGORY_TYPE_COLOR.put(CATEGORY_EDUCATION, context.getResources().getColor(R.color.event_category_education));
        CATEGORY_TYPE_COLOR.put(CATEGORY_MUSIC, context.getResources().getColor(R.color.event_category_music));
        CATEGORY_TYPE_COLOR.put(CATEGORY_DISCOUNT, context.getResources().getColor(R.color.event_category_discount));

        mEventDataSource = new EventDataSource(context, DATABASE_NAME, DATABASE_VERSION);
        mEventStatsDataSource = new EventStatsDataSource(context, DATABASE_NAME, DATABASE_VERSION);
        mEventToCalendarDataSource = new EventToCalendarDataSource(context, DATABASE_NAME, DATABASE_VERSION);
        mEventAdvertDataSource = new EventAdvertDataSource(context, DATABASE_NAME, DATABASE_VERSION);

        mBackgroundLoadTask = new Runnable() {
            @Override
            public void run() {
                int result = RESULT_OK;
                while (!Thread.interrupted() && mLoadedInBackgroundPage < getAvailablePageCount() && mLoadedInBackgroundPage <= PAGE_LIMIT_LOAD_IN_BACKGROUND() && result == RESULT_OK) {
                    try {
                        result = performEventsLoad(LOAD_IN_BACKGROUND);
                    } catch (@NonNull IOException | JSONException exp) {
                        //empty
                    }
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOAD_IN_BACKGROUND_NOTIFICATION));
                    VTAG.call("Background Load Task: " + mLoadedInBackgroundPage + " : " + getAvailablePageCount() + " : " + result);
                }
            }
        };


        mTimer = new Timer();

        mTimer.scheduleAtFixedRate(

                new TimerTask() {

                    @Override
                    public void run() {
                        if (getCurrentDayTimeInSeconds() % TimeUnit.MINUTES.toSeconds(1) == 0 && TimeUnit.SECONDS.toMinutes(getCurrentDayTime(mContext, false)) % mContext.getResources().getInteger(R.integer.event_time_precision_in_minutes) == 0) {
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(UPDATE_FIVE_MINUTE_TIMER_NOTIFICATION));
                        }
                    }
                },
                0,
                TimeUnit.SECONDS.toMillis(1));

        mEventDataSource.openForWrite();
        mEventStatsDataSource.openForWrite();
        mEventToCalendarDataSource.openForWrite();
        mEventAdvertDataSource.openForWrite();

        ArrayList<Event> allEvents = mEventDataSource.getAllEvents();
        for (Event event : allEvents) {
            if (!eventIsActual(event)) {
                mEventDataSource.removeEventById(event.id);
                mEventStatsDataSource.removeEventByEventId(event.id);
                mEventToCalendarDataSource.removeLinkByEventId(event.id);
            }
        }

        setEventsFromLocalDataSource(allEvents);
    }

    public static String getStartTimeAliasForAdvert(@NonNull Context context, @NonNull Event event) {
        return createEventStartTimeString(context, event.date, event.startAt, event.endAt, false, true).toLowerCase();
    }

    public static String getStartTimeAlias(@NonNull Context context, @NonNull Event event) {
        return createEventStartTimeString(context, event.date, event.startAt, event.endAt, false, false);
    }

    public static String getStartTimeKeywords(@NonNull Context context, @NonNull Event event) {
        return createEventStartTimeString(context, event.date, event.startAt, event.endAt, true, false);
    }

    private static String createEventStartTimeString(@NonNull Context context, long date, long startAt, long endAt, boolean createForKeywords, boolean creteForAdvert) {
        Resources resources = context.getResources();
        long currentTimeTimestamp = getCurrentDayTime(context, true);
        long startAfterSeconds = startAt - currentTimeTimestamp;
        long dateDelta = date - getCurrentDateTime(context, true);
        String startTimeAliasString;
        startAfterSeconds += dateDelta;
        if (startAfterSeconds <= 0) {
            if (endAt > currentTimeTimestamp || endAt == 0) {
                startTimeAliasString = resources.getString(createForKeywords ? R.string.going_right_now_keywords : creteForAdvert ? R.string.going_right_now_advert : R.string.going_right_now);
            } else {
                startTimeAliasString = resources.getString(R.string.already_ended);
            }
        } else if (startAfterSeconds <= MAXIMUM_TIME_PERIOD_FOR_DATE_ALIAS_SUPPORT) {
            long startAfterMinutesFull = TimeUnit.SECONDS.toMinutes(startAfterSeconds);
            long startAfterHours = TimeUnit.MINUTES.toHours(startAfterMinutesFull);
            long startAfterMinutes = startAfterMinutesFull % TimeUnit.HOURS.toMinutes(1);
            startTimeAliasString = resources.getString(R.string.going_in);
            if (startAfterHours > 0) {
                startTimeAliasString += String.format(" %d ч", startAfterHours);
            }
            if (startAfterMinutes > 0) {
                startTimeAliasString += String.format(" %d мин", startAfterMinutes);
            }
        } else {
            if (currentTimeTimestamp <= NIGHT_PERIOD[1] && startAt >= MORNING_PERIOD[0]) {
                dateDelta += TimeUnit.DAYS.toSeconds(1);
            }
            if (dateDelta == 0) {
                startTimeAliasString = String.format("%s %s", resources.getString(R.string.today), getDayPeriodAlias(startAt));
            } else if (dateDelta >= TimeUnit.DAYS.toSeconds(1) && dateDelta < TimeUnit.DAYS.toSeconds(2)) {
                startTimeAliasString = String.format("%s %s", resources.getString(R.string.tomorrow), getDayPeriodAlias(startAt));
            } else if (dateDelta >= TimeUnit.DAYS.toSeconds(2) && dateDelta < TimeUnit.DAYS.toSeconds(3)) {
                startTimeAliasString = resources.getString(R.string.day_after_tomorrow);
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(date));
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

    public static synchronized EventsModel getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new EventsModel(context);
        }
        return sInstance;
    }

    public static synchronized EventsModel getInstance() {
        assert sInstance != null;
        return sInstance;
    }

    public static long getCurrentDayTime(@NonNull Context context, boolean usePrecision) {
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

    private static long getCurrentDayTimeInSeconds() {
        return Calendar.getInstance().get(Calendar.SECOND);
    }

    public static long getCurrentDateTime(@NonNull Context context, boolean usePrecision) {
        Calendar calendar = Calendar.getInstance();
        long currentTime = TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis()) - getCurrentDayTime(context, false);
        if (usePrecision) {
            long precision = TimeUnit.MINUTES.toSeconds(context.getResources().getInteger(R.integer.event_time_precision_in_minutes));
            currentTime = ((int) currentTime / precision) * precision;
        }
        return currentTime + getTimeZoneOffsetInSeconds();
    }

    private static long getTimeZoneOffsetInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTimeZone().getRawOffset());
    }

    public static long getEventStartTimeInSeconds(@NonNull Event event) {
        return event.date + event.startAt - getTimeZoneOffsetInSeconds();
    }

    public static long getEventEndTimeInSeconds(@NonNull Event event) {
        return event.date + event.endAt - getTimeZoneOffsetInSeconds();
    }

    public void destroy() {
        mTimer.cancel();
    }

    private boolean FEATURE_LOAD_IN_BACKGROUND() {
        return mContext.getResources().getBoolean(R.bool.feature_load_in_background);
    }

    private int PAGE_LIMIT_LOAD_IN_BACKGROUND() {
        return mContext.getResources().getInteger(R.integer.page_limit_load_in_background);
    }

    @Nullable
    private EventToCalendarLink addEventToCalendarLink(@NonNull Event event, long calendarEventID) {
        return mEventToCalendarDataSource.addLink(event.id, calendarEventID);
    }

    private void removeEventToCalendarLinkByEventId(long eventId) {
        mEventToCalendarDataSource.removeLinkByEventId(eventId);
    }

    @Nullable
    private EventToCalendarLink getEventToCalendarLinkByEventId(long eventId) {
        return mEventToCalendarDataSource.getLinkByEventID(eventId);
    }

    public void registerForLoadInBackgroundResultNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, new IntentFilter(LOAD_IN_BACKGROUND_NOTIFICATION));
    }

    public void unregisterForLoadInBackgroundResultNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }

    public void registerForFiveMinuteUpdateNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, new IntentFilter(UPDATE_FIVE_MINUTE_TIMER_NOTIFICATION));
    }

    public void unregisterForFiveMinuteUpdateNotification(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }

    private boolean eventIsActual(@NonNull Event event) {
        return (event.date + event.endAt) > (getCurrentDayTime(mContext, true) + getCurrentDateTime(mContext, true));
    }

    @Nullable
    private Event getEventById(int id) {
        Event result = null;
        for (int i = 0; i < mEvents.size() && result == null; i++) {
            Event event = mEvents.get(i);
            if (event.id == id) {
                result = event;
            }
        }
        return result;
    }

    public void addEvents(ArrayList<Event> events) {
        mEvents.addAll(events);
    }

    @NonNull
    public ArrayList<Event> getEvents() {
        return mEvents;
    }

    public void setEvents(ArrayList<Event> events) {
        mEvents.clear();
        mEvents.addAll(events);
    }

    public boolean dataIsActual() {
        return mDataIsActual;
    }

    public boolean isAvailableLoad() {
        return mEvents.size() < mAvailableEventCount && !mReachEndOfDataList;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    @NonNull
    private ArrayList<Event> sortByStartTime(@NonNull ArrayList<Event> events) {
        Collections.sort(events, new EventNameComparator());
        return events;
    }

    private void setEventsFromLocalDataSource(@NonNull ArrayList<Event> allEvents) {
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

    @NonNull
    ArrayList<Event> performSearch(@NonNull String query) {
        String[] splitQuery = query.split(" ");
        ArrayList<String> queryList = new ArrayList<>();
        for (String item : splitQuery) {
            queryList.add(item + "*");
        }
        String queryResult = TextUtils.join(" ", queryList);
        return mEventDataSource.fullTextSearch(queryResult);
    }

    @Nullable
    EventApiCallResult performObtainEventStats(int eventId) {
        int result = RESULT_OK;
        Event event;
        EventStats eventStats = obtainEventStatsByEventId(eventId);
        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            try {
                Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_STATS);
                uriBuilder.appendQueryParameter("id", Integer.toString(eventId));
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonString = readStream(urlConnection.getInputStream());
                    urlConnection.disconnect();
                    JSONObject jsonRootObject = new JSONObject(jsonString);
                    eventStats.likeCount = jsonRootObject.getInt(EventFieldNameDictionary.LIKES);
                    eventStats.viewCount = jsonRootObject.getInt(EventFieldNameDictionary.VIEWS);
                }
            } catch (@NonNull IOException | JSONException exp) {
                result = RESULT_BAD;
            }
        }
        event = getEventById(eventId);
        if (event == null) {
            event = mEventDataSource.getByEventId(eventId);
        }
        mEventStatsDataSource.createOrUpdateEventStats(eventStats);
        return new EventApiCallResult(result, event);
    }

    @Nullable
    EventApiCallResult performObtainEvent(int eventId) {
        int result = RESULT_OK;
        Event event = getEventById(eventId);
        if (NowApplication.getState() == ONLINE && event == null) {
            try {
                Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_EVENT);
                uriBuilder.appendQueryParameter("id", Integer.toString(eventId));
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonString = readStream(urlConnection.getInputStream());
                    urlConnection.disconnect();
                    JSONObject jsonRootObject = new JSONObject(jsonString);
                    String response = jsonRootObject.getString("response");
                    if ("ok".equals(response)) {
                        JSONObject eventJsonObject = jsonRootObject.getJSONObject("event");
                        event = createEventFromJson(eventJsonObject);
                        createEventStats(event.id, eventJsonObject.optInt(EventFieldNameDictionary.VIEWS), eventJsonObject.optInt(EventFieldNameDictionary.LIKES));
                        mEventDataSource.createOrUpdateEvent(event);
                        setEvents(sortByStartTime(mEventDataSource.getAllEvents()));
                    } else {
                        result = RESULT_BAD;
                    }
                } else {
                    result = RESULT_BAD;
                }
            } catch (@NonNull IOException | JSONException exp) {
                result = RESULT_BAD;
            }
        }
        return new EventApiCallResult(result, event);
    }

    @Nullable
    EventApiCallResult performUpdateEventLike(int eventId) {
        int result = RESULT_OK;
        ArrayList<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("key", "78GlLJhL"));
        EventStats eventStats = obtainEventStatsByEventId(eventId);
        eventStats.myLikeStatus = LIKE_NOT_CONFIRMED;
        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            result = performPostApiCall(API_REQUEST_UPDATE_LIKE, eventId, parameters);
            if (result == RESULT_OK) {
                eventStats.myLikeStatus = LIKE_CONFIRMED;
            }
        }
        mEventStatsDataSource.createOrUpdateEventStats(eventStats);
        return new EventApiCallResult(result, null);
    }

    @Nullable
    EventApiCallResult performUpdateEventView(int eventId) {
        return new EventApiCallResult(performPostApiCall(API_REQUEST_UPDATE_VIEW, eventId, null), null);
    }

    private int performPostApiCall(String api, int eventId, @Nullable ArrayList<NameValuePair> parameters) {
        int result = RESULT_OK;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        ArrayList<NameValuePair> fullParameters = new ArrayList<>();
        fullParameters.add(new BasicNameValuePair("id", Integer.toString(eventId)));
        if (parameters != null && parameters.size() > 0) {
            fullParameters.addAll(parameters);
        }
        HttpPost httpPost = new HttpPost(getApiQuery(api));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(fullParameters));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            String jsonString = EntityUtils.toString(httpEntity);
            JSONObject jsonRootObject = new JSONObject(jsonString);
            String response = jsonRootObject.getString("response");
            if (!("ok".equals(response))) {
                result = RESULT_BAD;
            }
        } catch (@NonNull IOException | JSONException e) {
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

    int performEventsLoad(@Nullable String loadingType) throws IOException, JSONException {

        Integer result = RESULT_OK;

        boolean requestNewEvents = REQUEST_NEW_EVENTS.equals(loadingType);

        if (FEATURE_LOAD_IN_BACKGROUND()) {
            if (requestNewEvents) {
                if (mLoadedInBackgroundPage > 1 && mCurrentPage <= mLoadedInBackgroundPage) {
                    if (mCurrentPage <= getAvailablePageCount()) {
                        mCurrentPage = mLoadedInBackgroundPage;
                        setEvents(sortByStartTime(mEventDataSource.getAllEvents()));
                        return result;
                    }
                } else {
                    mReachEndOfDataList = true;
                }
            }
        }

        boolean loadInBackground = LOAD_IN_BACKGROUND.equals(loadingType);
        boolean refreshEvents = REFRESH_EVENTS.equals(loadingType) || loadingType == null;

        ArrayList<Event> eventList = new ArrayList<>();

        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_EVENTS);
            if (requestNewEvents || loadInBackground) {
                Event lastEvent = requestNewEvents ? mEvents.get(mEvents.size() - 1) : mLastAddedInBackgroundEvent;
                if (lastEvent != null) {
                    String lastEventId = String.format("%d", lastEvent.id);
                    uriBuilder
                            .appendQueryParameter("fl", "0")
                            .appendQueryParameter("date", String.format("%d", lastEvent.date))
                            .appendQueryParameter("startAt", String.format("%d", lastEvent.startAt))
                            .appendQueryParameter("id", lastEventId);
                }
            } else {
                uriBuilder
                        .appendQueryParameter("fl", "1")
                        .appendQueryParameter("date", String.format("%d", getCurrentDateTime(mContext, true)))
                        .appendQueryParameter("startAt", String.format("%d", getCurrentDayTime(mContext, true)));
            }
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String jsonString = readStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                JSONObject jsonRootObject = new JSONObject(jsonString);
                String response = jsonRootObject.getString("response");
                mAvailableEventCount = jsonRootObject.getInt("events_count");

                if ("ok".equals(response)) {
                    JSONArray eventJsonArray = jsonRootObject.getJSONArray(TAG_EVENTS);
                    JSONArray eventAdvertsJsonArray = jsonRootObject.getJSONArray(TAG_ADVERTS);
                    for (int i = 0; i < eventJsonArray.length(); i++) {
                        JSONObject eventJsonObject = eventJsonArray.getJSONObject(i);
                        Event event = createEventFromJson(eventJsonObject);
                        createEventStats(event.id, eventJsonObject.optInt(EventFieldNameDictionary.VIEWS), eventJsonObject.optInt(EventFieldNameDictionary.LIKES));
                        eventList.add(event);
                    }
                    if (eventAdvertsJsonArray.length() > 0) {
                        mEventAdvertDataSource.clear();
                        for (int i = 0; i < eventAdvertsJsonArray.length(); i++) {
                            JSONObject eventAdvertJsonObject = eventAdvertsJsonArray.getJSONObject(i);
                            EventAdvert eventAdvert = createEventAdvertFromJson(eventAdvertJsonObject);
                            mEventAdvertDataSource.createEventAdvert(eventAdvert);
                        }
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
                            mLastAddedInBackgroundEvent = eventList.get(eventList.size() - 1);
                            if (refreshEvents && mEventsCountPerPage == 0) {
                                mEventsCountPerPage = eventList.size();
                            }
                            if (mLoadedInBackgroundPage == 1) {
                                mEventDataSource.clear();
                                loadInBackground();
                            }
                            for (int i = 0; i < eventList.size(); i++) {
                                Event event = eventList.get(i);
                                mEventDataSource.createOrUpdateEvent(event);
                            }
                            NowApplication.updateDataTimestamp();
                        }
                        mDataIsActual = true;
                    } else {
                        mReachEndOfDataList = !loadInBackground;
                        result = DATA_IS_EMPTY;
                    }
                } else {
                    setEventsFromLocalDataSource(mEventDataSource.getAllEvents());
                    mAvailableEventCount = getEvents().size();
                }
            }
        }
        return result;
    }

    @NonNull
    private Event createEventFromJson(@NonNull JSONObject jsonObject) {
        Event event = new Event();
        event.date = jsonObject.optLong(EventFieldNameDictionary.DATE, 0);
        event.eventDescription = jsonObject.optString(EventFieldNameDictionary.EVENT_DESCRIPTION);
        event.placeName = jsonObject.optString(EventFieldNameDictionary.PLACE_NAME);
        event.placeId = jsonObject.optInt(EventFieldNameDictionary.PLACE_ID);
        event.id = jsonObject.optInt(EventFieldNameDictionary.ID);
        event.category = jsonObject.optString(EventFieldNameDictionary.EVENT_CATEGORY);
        event.entrance = jsonObject.optString(EventFieldNameDictionary.ENTRANCE);
        event.address = jsonObject.optString(EventFieldNameDictionary.ADDRESS);
        event.phone = jsonObject.optString(EventFieldNameDictionary.PHONE);
        event.site = jsonObject.optString(EventFieldNameDictionary.SITE);
        event.email = jsonObject.optString(EventFieldNameDictionary.EMAIL);
        event.lat = jsonObject.optDouble(EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE);
        event.lng = jsonObject.optDouble(EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE);
        event.name = jsonObject.optString(EventFieldNameDictionary.NAME);
        event.startAt = jsonObject.optLong(EventFieldNameDictionary.START_AT);
        event.endAt = jsonObject.optLong(EventFieldNameDictionary.END_AT);
        event.posterThumb = jsonObject.optString(EventFieldNameDictionary.POSTER_THUMB);
        event.posterBlur = jsonObject.optString(EventFieldNameDictionary.POSTER_BLUR);
        event.posterOriginal = jsonObject.optString(EventFieldNameDictionary.POSTER_ORIGINAL);
        event.logoOriginal = jsonObject.optString(EventFieldNameDictionary.LOGO_ORIGINAL);
        event.logoThumb = jsonObject.optString(EventFieldNameDictionary.LOGO_THUMB);
        event.allNightParty = jsonObject.optBoolean(EventFieldNameDictionary.ALL_NIGHT_PARTY) ? 1 : 0;
        return event;
    }

    @NonNull
    private EventAdvert createEventAdvertFromJson(@NonNull JSONObject jsonObject) {
        EventAdvert eventAdvert = new EventAdvert();
        eventAdvert.id = jsonObject.optInt(EventFieldNameDictionary.ID, 0);
        eventAdvert.placeName = jsonObject.optString(EventFieldNameDictionary.ADVERT.PLACE_NAME);
        eventAdvert.startAt = jsonObject.optLong(EventFieldNameDictionary.ADVERT.START_AT);
        eventAdvert.name = jsonObject.optString(EventFieldNameDictionary.ADVERT.NAME);
        eventAdvert.eventId = jsonObject.optInt(EventFieldNameDictionary.ADVERT.EVENT_ID);
        eventAdvert.adType = jsonObject.optString(EventFieldNameDictionary.ADVERT.AD_TYPE);
        eventAdvert.link = jsonObject.optString(EventFieldNameDictionary.ADVERT.LINK);
        eventAdvert.showChanceHigh = jsonObject.optInt(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH);
        eventAdvert.showChanceLow = jsonObject.optInt(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW);
        eventAdvert.logoThumb = jsonObject.optString(EventFieldNameDictionary.ADVERT.LOGO_THUMB);
        return eventAdvert;
    }

    private String readStream(@NonNull InputStream in) {
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException exp) {
            //empty
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException exp) {
                    //empty
                }
            }
        }
        return response.toString();
    }

    @Nullable
    public EventToCalendarLink performCalendarFunctionality(int method, int eventId) {
        EventToCalendarLink result = null;
        long calendarEventID;
        ContentResolver contentResolver = mContext.getContentResolver();
        if (method == EventToCalendarLoader.CHECK) {
            result = getEventToCalendarLinkByEventId(eventId);
            if (result != null) {
                calendarEventID = result.getCalendarEventID();
                Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null, null, null);
                if (!cursor.moveToFirst()) {
                    result = null;
                    removeEventToCalendarLinkByEventId(eventId);
                }
                cursor.close();
            }
        } else if (method == EventToCalendarLoader.ADD) {
            Event event = getEventById(eventId);
            TimeZone timeZone = Calendar.getInstance().getTimeZone();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, TimeUnit.SECONDS.toMillis(getEventStartTimeInSeconds(event)));
            values.put(CalendarContract.Events.DTEND, TimeUnit.SECONDS.toMillis(getEventEndTimeInSeconds(event)));
            values.put(CalendarContract.Events.TITLE, event.name);
            values.put(CalendarContract.Events.EVENT_LOCATION, event.placeName);
            if (!"".equals(event.email)) {
                values.put(CalendarContract.Events.ORGANIZER, event.email);
            }
            values.put(CalendarContract.Events.DESCRIPTION, event.eventDescription);
            values.put(CalendarContract.Events.EVENT_COLOR, getCategoryColor(event.category));
            values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
            values.put(CalendarContract.Events.CALENDAR_ID, 1);
            Uri insertToCalendarURI = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
            calendarEventID = Long.parseLong(insertToCalendarURI.getLastPathSegment());
            result = addEventToCalendarLink(event, calendarEventID);
        } else if (method == EventToCalendarLoader.REMOVE) {
            result = getEventToCalendarLinkByEventId(eventId);
            if (result != null) {
                calendarEventID = result.getCalendarEventID();
                Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null, null, null);
                removeEventToCalendarLinkByEventId(eventId);
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

    @Nullable
    public EventStats obtainEventStatsByEventId(int eventId) {
        EventStats eventStats = mEventStatsDataSource.getByEventId(eventId);
        if (eventStats == null) {
            eventStats = new EventStats();
            eventStats.id = eventId;
            mEventStatsDataSource.createOrUpdateEventStats(eventStats);
        }
        return eventStats;
    }

    private void createEventStats(int eventId, int viewCount, int likeCount) {
        EventStats eventStats = mEventStatsDataSource.getByEventId(eventId);
        if (eventStats == null) {
            eventStats = new EventStats();
            eventStats.id = eventId;
            eventStats.viewCount = viewCount;
            eventStats.likeCount = likeCount;
            mEventStatsDataSource.createOrUpdateEventStats(eventStats);
        }
    }

    @Nullable
    public EventAdvert generateAdvertExcludeByEventId(int eventId) {
        long currentTime = getCurrentDayTime(mContext, true) + getCurrentDateTime(mContext, true);
        ArrayList<EventAdvert> eventAdvertsSource = mEventAdvertDataSource.getAllEventAdverts();
        ArrayList<EventAdvert> eventAdverts = new ArrayList<>();
        EventAdvert eventAdvert;
        EventAdvert result = null;
        int i = 0;
        for (; i < eventAdvertsSource.size(); i++) {
            eventAdvert = eventAdvertsSource.get(i);
            if (eventAdvert.eventId != eventId && checkOnEventAvailableInOffline(eventAdvert.eventId) && currentTime < eventAdvert.startAt) {
                eventAdverts.add(eventAdvert);
            }
        }
        if (eventAdverts.size() > 0) {
            int dice = new Random().nextInt(100);
            for (i = 0; result == null && i < eventAdverts.size(); i++) {
                eventAdvert = eventAdverts.get(i);
                if (eventAdvert.showChanceHigh != 0 && dice >= eventAdvert.showChanceLow && dice < eventAdvert.showChanceHigh) {
                    result = eventAdvert;
                }
            }
        }
        /*for debug
        if (result == null && eventAdvertsSource.size() > 0) {
            result = eventAdvertsSource.get(0);
        }
        */
        return result;
    }

    private boolean checkOnEventAvailableInOffline(int eventId) {
        return NowApplication.getState() != OFFLINE || mEventDataSource.getByEventId(eventId) != null;
    }

    private class EventNameComparator implements Comparator<Event> {
        public int compare(@NonNull Event left, @NonNull Event right) {
            return Long.valueOf(left.date + left.startAt).compareTo(right.date + right.startAt);
        }
    }
}
