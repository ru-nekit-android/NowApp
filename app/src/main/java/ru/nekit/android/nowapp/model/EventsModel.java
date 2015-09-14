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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import ru.nekit.android.nowapp.model.db.EventAdvertDataSource;
import ru.nekit.android.nowapp.model.db.EventDataSource;
import ru.nekit.android.nowapp.model.db.EventStatsDataSource;
import ru.nekit.android.nowapp.model.db.EventToCalendarDataSource;
import ru.nekit.android.nowapp.model.loaders.EventToCalendarLoader;
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.model.vo.EventAdvert;
import ru.nekit.android.nowapp.model.vo.EventStats;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;
import ru.nekit.android.nowapp.utils.VTAG;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.OFFLINE;
import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

/**
 * Created by chuvac on 15.03.15.
 */
public class EventsModel {

    public static final int RESULT_OK = 0;
    public static final int RESULT_BAD = -1;
    public static final int INTERNAL_ERROR = -2;
    public static final int LIKE_CONFIRMED = 2;
    public static final int LIKE_NOT_CONFIRMED = 1;
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
    private static final String API_ROOT = "api/v2";
    private static final String API_EVENT = API_ROOT + "/event";
    private static final String API_DEVICE = API_ROOT + "/device";
    private static final String API_REQUEST_REGISTER_DEVICE = API_DEVICE + "/register";
    private static final String API_REQUEST_GET_EVENTS = API_EVENT + "s";
    private static final String API_REQUEST_GET_ADVERTS = API_ROOT + "/adverts";
    private static final String API_REQUEST_GET_EVENT = API_EVENT;
    private static final String API_REQUEST_GET_STATISTICS = API_EVENT + "/statistics";
    private static final String API_REQUEST_LIKE = API_EVENT + "/like";
    private static final int COUNT = 12;

    public String deviceToken;

    private static final String DATABASE_NAME = "nowapp.db";
    private static final int DATABASE_VERSION = 14;
    private static final HashMap<String, String> CATEGORY_TYPE_KEYWORDS = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE = new HashMap<>();
    private static final String CATEGORY_SPORT = "category_sport";
    private static final String CATEGORY_ENTERTAINMENT = "category_entertainment";
    private static final String CATEGORY_OTHER = "category_other";
    private static final String CATEGORY_EDUCATION = "category_education";
    private static final String CATEGORY_DISCOUNT = "category_discount";
    private static final String CATEGORY_CULTURE = "category_culture";

    private static final HashMap<String, Integer> CATEGORY_TYPE_COLOR = new HashMap<>();
    private static final HashMap<String, Integer> CATEGORY_TYPE_BIG = new HashMap<>();
    @NonNull
    private static ArrayList<Pair<String, long[]>> PERIODS = new ArrayList<Pair<String, long[]>>(4) {{
        add(new Pair<>("ночью", NIGHT_PERIOD));
        add(new Pair<>("утром", MORNING_PERIOD));
        add(new Pair<>("днем", DAY_PERIOD));
        add(new Pair<>("вечером", EVENING_PERIOD));
    }};
    @NonNull
    private final Context mContext;
    @NonNull
    private final LocalBroadcastManager mLocalBroadcastManager;
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
    private int mCurrentPage;
    private boolean mDataIsActual;
    private int mLoadedInBackgroundPage;
    private int mEventsCountPerPage;
    private Event mLastAddedInBackgroundEvent;
    private Thread mBackgroundThread;
    private Boolean mHasNext;

    public EventsModel(@NonNull final Context context) {
        mContext = context;
        mEvents = new ArrayList<>();
        mCurrentPage = 1;
        mLoadedInBackgroundPage = 1;
        mEventsCountPerPage = 0;
        mHasNext = true;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);

        CATEGORY_TYPE.put(CATEGORY_SPORT, R.drawable.event_category_sport);
        CATEGORY_TYPE.put(CATEGORY_ENTERTAINMENT, R.drawable.event_category_entertainment);
        CATEGORY_TYPE.put(CATEGORY_OTHER, R.drawable.event_category_other);
        CATEGORY_TYPE.put(CATEGORY_EDUCATION, R.drawable.event_category_education);
        CATEGORY_TYPE.put(CATEGORY_DISCOUNT, R.drawable.event_category_discount);
        CATEGORY_TYPE.put(CATEGORY_CULTURE, R.drawable.event_category_culture);

        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_SPORT, context.getResources().getString(R.string.event_category_sport_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_ENTERTAINMENT, context.getResources().getString(R.string.event_category_entertainment_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_OTHER, context.getResources().getString(R.string.event_category_other_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_EDUCATION, context.getResources().getString(R.string.event_category_education_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_CULTURE, context.getResources().getString(R.string.event_category_culture_keywords));
        CATEGORY_TYPE_KEYWORDS.put(CATEGORY_DISCOUNT, context.getResources().getString(R.string.event_category_discount_keywords));

        CATEGORY_TYPE_BIG.put(CATEGORY_SPORT, R.drawable.event_category_sport_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_ENTERTAINMENT, R.drawable.event_category_entertainment_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_OTHER, R.drawable.event_category_other_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_EDUCATION, R.drawable.event_category_education_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_DISCOUNT, R.drawable.event_category_discount_big);
        CATEGORY_TYPE_BIG.put(CATEGORY_CULTURE, R.drawable.event_category_culture_big);

        CATEGORY_TYPE_COLOR.put(CATEGORY_SPORT, context.getResources().getColor(R.color.event_category_sport));
        CATEGORY_TYPE_COLOR.put(CATEGORY_ENTERTAINMENT, context.getResources().getColor(R.color.event_category_entertainment));
        CATEGORY_TYPE_COLOR.put(CATEGORY_OTHER, context.getResources().getColor(R.color.event_category_other));
        CATEGORY_TYPE_COLOR.put(CATEGORY_EDUCATION, context.getResources().getColor(R.color.event_category_education));
        CATEGORY_TYPE_COLOR.put(CATEGORY_CULTURE, context.getResources().getColor(R.color.event_category_culture));
        CATEGORY_TYPE_COLOR.put(CATEGORY_DISCOUNT, context.getResources().getColor(R.color.event_category_discount));

        mEventDataSource = new EventDataSource(context, DATABASE_NAME, DATABASE_VERSION);
        mEventStatsDataSource = new EventStatsDataSource(context, DATABASE_NAME, DATABASE_VERSION);
        mEventToCalendarDataSource = new EventToCalendarDataSource(context, DATABASE_NAME, DATABASE_VERSION);
        mEventAdvertDataSource = new EventAdvertDataSource(context, DATABASE_NAME, DATABASE_VERSION);

        mBackgroundLoadTask = new Runnable() {
            @Override
            public void run() {
                int result = RESULT_OK;
                while (!Thread.interrupted() && mHasNext && mLoadedInBackgroundPage <= PAGE_LIMIT_LOAD_IN_BACKGROUND() && result == RESULT_OK) {
                    try {
                        result = performEventsLoad(LOAD_IN_BACKGROUND);
                    } catch (@NonNull IOException | JSONException exp) {
                        //empty
                    }
                    sendBroadcast(LOAD_IN_BACKGROUND_NOTIFICATION);
                    VTAG.call("Background Load Task: " + mLoadedInBackgroundPage + " : " + result);
                }
            }
        };

        Timer mTimer = new Timer();

        mTimer.scheduleAtFixedRate(

                new TimerTask() {

                    @Override
                    public void run() {
                        long dayTime = getDayTimeInSeconds();
                        if (dayTime % TimeUnit.MINUTES.toSeconds(1) == 0 && TimeUnit.SECONDS.toMinutes(dayTime) % mContext.getResources().getInteger(R.integer.event_time_precision_in_minutes) == 0) {
                            sendBroadcast(UPDATE_FIVE_MINUTE_TIMER_NOTIFICATION);
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

    private void sendBroadcast(String action) {
        mLocalBroadcastManager.sendBroadcast(new Intent(action));
    }

    public static String getStartTimeAliasForAdvert(@NonNull Context context, @NonNull Event event) {
        return createEventStartTimeString(context, event, false, true).toLowerCase();
    }

    public static String getStartTimeAlias(@NonNull Context context, @NonNull Event event) {
        return createEventStartTimeString(context, event, false, false);
    }

    public static String getStartTimeKeywords(@NonNull Context context, @NonNull Event event) {
        return createEventStartTimeString(context, event, true, false);
    }

    private static String createEventStartTimeString(@NonNull Context context, Event event, boolean createForKeywords, boolean creteForAdvert) {
        Resources resources = context.getResources();
        long timeStampInSeconds = getTimeStampInSeconds();
        long startAfterSeconds = event.startAt - timeStampInSeconds;
        String startTimeAliasString;
        if (startAfterSeconds <= 0) {
            if (event.endAt > timeStampInSeconds || event.endAt == 0) {
                startTimeAliasString = resources.getString(createForKeywords ? R.string.going_right_now_keywords : creteForAdvert ? R.string.going_right_now_advert : R.string.going_right_now);
            } else {
                startTimeAliasString = resources.getString(R.string.already_ended);
            }
        } else if (startAfterSeconds <= MAXIMUM_TIME_PERIOD_FOR_DATE_ALIAS_SUPPORT) {
            startAfterSeconds = getTimeWithPrecision(context, startAfterSeconds);
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
            long currentDate = TimeUnit.SECONDS.toDays(timeStampInSeconds);
            long eventDate = TimeUnit.SECONDS.toDays(event.startAt);
            if (timeStampInSeconds <= NIGHT_PERIOD[1]) {
                eventDate--;
            }
            long dateDelta = eventDate - currentDate;
            if (dateDelta == 0) {
                startTimeAliasString = String.format("%s %s", resources.getString(R.string.today), getDayPeriodAlias(getEventDayTimeInSeconds(event)));
            } else if (dateDelta == 1) {
                startTimeAliasString = String.format("%s %s", resources.getString(R.string.tomorrow), getDayPeriodAlias(getEventDayTimeInSeconds(event)));
            } else if (dateDelta == 2) {
                startTimeAliasString = resources.getString(R.string.day_after_tomorrow);
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(calendar.getTimeZone());
                calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(event.startAt));
                startTimeAliasString = String.format("%s %s", calendar.get(Calendar.DAY_OF_MONTH), new DateFormatSymbols().getMonths()[calendar.get(Calendar.MONTH)].toLowerCase());
            }
        }
        return startTimeAliasString;
    }

    private static String getDayPeriodAlias(long time) {
        String alias = PERIODS.get(0).first;
        for (int i = 1; i < PERIODS.size(); i++) {
            long[] periodTime = PERIODS.get(i).second;
            if (time >= periodTime[0] && time <= periodTime[1]) {
                alias = PERIODS.get(i).first;
            }
        }
        return alias;
    }

    public static int getCategoryColor(String category) {
        return CATEGORY_TYPE_COLOR.get(category);
    }

    public static int getCategoryDrawable(String category) {
        Integer value = CATEGORY_TYPE.get(category);
        return value == null ? 0 : value;
    }

    public static int getCategoryBigDrawable(String category) {
        return CATEGORY_TYPE_BIG.get(category);
    }

    @NonNull
    public static String getCategoryKeywords(String category) {
        String value = CATEGORY_TYPE_KEYWORDS.get(category);
        return value == null ? "" : value;
    }

    public static long getEventDayTimeInSeconds(@NonNull Event event) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(calendar.getTimeZone());
        calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(event.startAt));
        return TimeUnit.HOURS.toSeconds(calendar.get(Calendar.HOUR_OF_DAY)) + TimeUnit.MINUTES.toSeconds(calendar.get(Calendar.MINUTE));
    }

    public static long getTimeStampInSeconds() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(calendar.getTimeZone());
        return TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis());
    }

    public static long getTimeWithPrecision(@NonNull Context context, long time) {
        long precision = TimeUnit.MINUTES.toSeconds(context.getResources().getInteger(R.integer.event_time_precision_in_minutes));
        time = ((int) time / precision) * precision;
        return time;
    }

    private static long getDayTimeInSeconds() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(calendar.getTimeZone());
        return TimeUnit.HOURS.toSeconds(calendar.get(Calendar.HOUR_OF_DAY)) + TimeUnit.MINUTES.toSeconds(calendar.get(Calendar.MINUTE)) + calendar.get(Calendar.SECOND);
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
        mLocalBroadcastManager.registerReceiver(receiver, new IntentFilter(LOAD_IN_BACKGROUND_NOTIFICATION));
    }

    public void unregisterForLoadInBackgroundResultNotification(BroadcastReceiver receiver) {
        mLocalBroadcastManager.unregisterReceiver(receiver);
    }

    public void registerForFiveMinuteUpdateNotification(BroadcastReceiver receiver) {
        mLocalBroadcastManager.registerReceiver(receiver, new IntentFilter(UPDATE_FIVE_MINUTE_TIMER_NOTIFICATION));
    }

    public void unregisterForFiveMinuteUpdateNotification(BroadcastReceiver receiver) {
        mLocalBroadcastManager.unregisterReceiver(receiver);
    }

    private boolean eventIsActual(@NonNull Event event) {
        return event.endAt > getDayTimeInSeconds();
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
        return mHasNext;
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

    @NonNull
    public ArrayList<Event> performSearch(String query) {
        String[] splitQuery = query.split(" ");
        ArrayList<String> queryList = new ArrayList<>();
        for (String item : splitQuery) {
            queryList.add(item + "*");
        }
        String queryString = TextUtils.join(" ", queryList);
        assert queryString != null;
        return mEventDataSource.fullTextSearch(queryString);
    }

    @Nullable
    public EventApiCallResult performObtainEventStats(int eventId) {
        int result = RESULT_OK;
        Event event;
        EventStats eventStats = obtainEventStatsByEventId(eventId);
        if (NowApplication.getInstance().getState() == NowApplication.APP_STATE.ONLINE) {
            try {
                Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_STATISTICS);
                uriBuilder.appendQueryParameter("id", Integer.toString(eventId));
                uriBuilder.appendQueryParameter("token", deviceToken);
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonString = readStream(urlConnection.getInputStream());
                    urlConnection.disconnect();
                    JSONObject jsonRootObject = new JSONObject(jsonString);
                    String code = jsonRootObject.getString("code");
                    if ("Success".equals(code)) {
                        if (eventStats != null) {
                            JSONObject jsonStatisticsObject = jsonRootObject.getJSONObject("data").getJSONObject("statistics");
                            eventStats.likedByMe = jsonStatisticsObject.getBoolean(EventFieldNameDictionary.LIKED_BY_ME);
                            eventStats.likeCount = jsonStatisticsObject.getInt(EventFieldNameDictionary.LIKES);
                            eventStats.viewCount = jsonStatisticsObject.getInt(EventFieldNameDictionary.VIEWS);
                        }
                    }
                }
            } catch (@NonNull IOException | JSONException exp) {
                result = INTERNAL_ERROR;
            }
        }
        event = getEventById(eventId);
        if (event == null) {
            event = mEventDataSource.getByEventId(eventId);
        }
        if (eventStats != null) {
            mEventStatsDataSource.createOrUpdateEventStats(eventStats);
        }
        return new EventApiCallResult(result, event);
    }

    @Nullable
    public EventApiCallResult performObtainEvent(int eventId) {
        int result = RESULT_OK;
        Event event = getEventById(eventId);
        if (NowApplication.getInstance().getState() == ONLINE && event == null) {
            try {
                Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_EVENT);
                uriBuilder.appendQueryParameter("id", Integer.toString(eventId));
                uriBuilder.appendQueryParameter("token", deviceToken);
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonString = readStream(urlConnection.getInputStream());
                    urlConnection.disconnect();
                    JSONObject jsonRootObject = new JSONObject(jsonString);
                    String code = jsonRootObject.getString("code");
                    if ("Success".equals(code)) {
                        JSONObject eventJsonObject = jsonRootObject.getJSONObject("data").getJSONObject("event");
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
                result = INTERNAL_ERROR;
            }
        }
        return new EventApiCallResult(result, event);
    }

    public int performRegisterDevice() {
        int result = RESULT_OK;
        if (NowApplication.getInstance().getState() == ONLINE) {
            try {
                Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_REGISTER_DEVICE);
                uriBuilder.appendQueryParameter("city_id", Integer.toString(1));
                uriBuilder.appendQueryParameter("device_id", Settings.Secure.getString(mContext.getContentResolver(),
                        Settings.Secure.ANDROID_ID));
                uriBuilder.appendQueryParameter("platform", "android");
                uriBuilder.appendQueryParameter("version", Integer.toString(NowApplication.VERSION));
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonString = readStream(urlConnection.getInputStream());
                    urlConnection.disconnect();
                    JSONObject jsonRootObject = new JSONObject(jsonString);
                    String code = jsonRootObject.getString("code");
                    if ("Success".equals(code)) {
                        deviceToken = jsonRootObject.getJSONObject("data").getString("token");
                    } else {
                        result = RESULT_BAD;
                    }
                } else {
                    result = RESULT_BAD;
                }
            } catch (@NonNull IOException | JSONException exp) {
                result = INTERNAL_ERROR;
            }
        }
        return result;
    }

    @Nullable
    public EventApiCallResult performEventLike(int eventId) {
        int result = RESULT_OK;
        Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_LIKE);
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String postParameters = "id=" + eventId + "&token=" + deviceToken;
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(postParameters);
            out.close();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String jsonString = readStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                JSONObject jsonRootObject = new JSONObject(jsonString);
                String code = jsonRootObject.getString("code");
                EventStats eventStats = obtainEventStatsByEventId(eventId);
                assert eventStats != null;
                eventStats.myLikeStatus = LIKE_NOT_CONFIRMED;
                if ("Success".equals(code)) {
                    eventStats.likedByMe = true;
                    eventStats.myLikeStatus = LIKE_CONFIRMED;
                } else {
                    result = RESULT_BAD;
                }
                mEventStatsDataSource.createOrUpdateEventStats(eventStats);
            }
        } catch (@NonNull IOException | JSONException exp) {
            result = INTERNAL_ERROR;
        }
        return new EventApiCallResult(result, null);
    }

    private Uri.Builder createApiUriBuilder(String apiMethod) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http");
        builder.authority(SITE_NAME);
        builder.path(apiMethod);
        return builder;
    }

    void loadAdverts() {
        if (NowApplication.getInstance().getState() == NowApplication.APP_STATE.ONLINE) {
            Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_ADVERTS);
            uriBuilder.appendQueryParameter("token", deviceToken);
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String jsonString = readStream(urlConnection.getInputStream());
                    urlConnection.disconnect();
                    JSONObject jsonRootObject = new JSONObject(jsonString);
                    String code = jsonRootObject.getString("code");
                    if ("Success".equals(code)) {
                        JSONObject jsonDataObject = jsonRootObject.getJSONObject("data");
                        JSONArray eventAdvertsJsonArray = jsonDataObject.getJSONArray(TAG_ADVERTS);
                        if (eventAdvertsJsonArray.length() > 0) {
                            mEventAdvertDataSource.clear();
                            for (int i = 0; i < eventAdvertsJsonArray.length(); i++) {
                                mEventAdvertDataSource.createEventAdvert(createEventAdvertFromJson(eventAdvertsJsonArray.getJSONObject(i)));
                            }
                        }
                    }
                }
            } catch (IOException | JSONException exp) {
                //empty
            }
        }
    }

    public int performEventsLoad(@Nullable String loadingType) throws IOException, JSONException {
        if (deviceToken == null) {
            performRegisterDevice();
        }
        int result = RESULT_OK;

        boolean requestNewEvents = REQUEST_NEW_EVENTS.equals(loadingType);

        if (FEATURE_LOAD_IN_BACKGROUND()) {
            if (requestNewEvents) {
                if (mLoadedInBackgroundPage > 1 && mCurrentPage <= mLoadedInBackgroundPage) {
                    mCurrentPage = mLoadedInBackgroundPage;
                    setEvents(sortByStartTime(mEventDataSource.getAllEvents()));
                    return result;
                }
            }
        }

        boolean loadInBackground = LOAD_IN_BACKGROUND.equals(loadingType);
        boolean refreshEvents = REFRESH_EVENTS.equals(loadingType) || loadingType == null;

        if (refreshEvents) {
            loadAdverts();
        }

        ArrayList<Event> eventList = new ArrayList<>();

        if (NowApplication.getInstance().getState() == NowApplication.APP_STATE.ONLINE) {
            Uri.Builder uriBuilder = createApiUriBuilder(API_REQUEST_GET_EVENTS);
            if (requestNewEvents || loadInBackground) {
                Event lastEvent = requestNewEvents ? mEvents.get(mEvents.size() - 1) : mLastAddedInBackgroundEvent;
                if (lastEvent != null) {
                    uriBuilder.appendQueryParameter("start_at", Long.toString(lastEvent.startAt));
                }
            }
            uriBuilder.appendQueryParameter("n", Integer.toString(COUNT));
            uriBuilder.appendQueryParameter("token", deviceToken);
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriBuilder.build().toString()).openConnection();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String jsonString = readStream(urlConnection.getInputStream());
                urlConnection.disconnect();
                JSONObject jsonRootObject = new JSONObject(jsonString);
                String code = jsonRootObject.getString("code");
                if ("Success".equals(code)) {
                    JSONObject jsonDataObject = jsonRootObject.getJSONObject("data");
                    mHasNext = jsonDataObject.getBoolean("hasNext");
                    JSONArray eventJsonArray = jsonDataObject.getJSONArray(TAG_EVENTS);
                    for (int i = 0; i < eventJsonArray.length(); i++) {
                        JSONObject eventJsonObject = eventJsonArray.getJSONObject(i);
                        Event event = createEventFromJson(eventJsonObject);
                        createEventStats(event.id, eventJsonObject.optInt(EventFieldNameDictionary.VIEWS), eventJsonObject.optInt(EventFieldNameDictionary.LIKES));
                        eventList.add(event);
                    }
                    if (eventList.size() > 0) {
                        if (requestNewEvents) {
                            addEvents(eventList);
                            mCurrentPage++;
                        } else if (refreshEvents) {
                            setEvents(eventList);
                            mCurrentPage = 1;
                            mLoadedInBackgroundPage = 1;
                        } else if (loadInBackground) {
                            addEvents(eventList);
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
                            NowApplication.getInstance().updateDataTimestamp();
                        }
                        mDataIsActual = true;
                    }
                } else {
                    setEventsFromLocalDataSource(mEventDataSource.getAllEvents());
                }
            }
        }
        return result;
    }

    @NonNull
    private Event createEventFromJson(@NonNull JSONObject jsonObject) {
        Event event = new Event();
        event.eventDescription = jsonObject.optString(EventFieldNameDictionary.EVENT_DESCRIPTION);
        event.flayer = jsonObject.optString(EventFieldNameDictionary.FLAYER);
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
        eventAdvert.flayer = jsonObject.optString(EventFieldNameDictionary.ADVERT.FLAYER);
        eventAdvert.eventStartAt = jsonObject.optLong(EventFieldNameDictionary.ADVERT.EVENT_START_AT);
        eventAdvert.eventEndAt = jsonObject.optLong(EventFieldNameDictionary.ADVERT.EVENT_END_AT);
        eventAdvert.advertStartAt = jsonObject.optLong(EventFieldNameDictionary.ADVERT.ADVERT_START_AT);
        eventAdvert.advertEndAt = jsonObject.optLong(EventFieldNameDictionary.ADVERT.ADVERT_END_AT);
        eventAdvert.text = jsonObject.optString(EventFieldNameDictionary.ADVERT.TEXT);
        eventAdvert.eventId = jsonObject.optInt(EventFieldNameDictionary.ADVERT.EVENT_ID);
        eventAdvert.posterBlur = jsonObject.optString(EventFieldNameDictionary.ADVERT.POSTER_BLUR);
        eventAdvert.posterOriginal = jsonObject.optString(EventFieldNameDictionary.ADVERT.POSTER_ORIGINAL);
        eventAdvert.posterThumb = jsonObject.optString(EventFieldNameDictionary.ADVERT.POSTER_THUMB);
        eventAdvert.logoOriginal = jsonObject.optString(EventFieldNameDictionary.ADVERT.LOGO_ORIGINAL);
        eventAdvert.logoThumb = jsonObject.optString(EventFieldNameDictionary.ADVERT.LOGO_THUMB);
        eventAdvert.showChanceHigh = jsonObject.optInt(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH);
        eventAdvert.showChanceLow = jsonObject.optInt(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW);
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
            //
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException exp) {
                    //
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
                assert cursor != null;
                if (!cursor.moveToFirst()) {
                    result = null;
                    removeEventToCalendarLinkByEventId(eventId);
                }
                cursor.close();
            }
        } else if (method == EventToCalendarLoader.ADD) {
            Event event = getEventById(eventId);
            if (event != null) {
                TimeZone timeZone = Calendar.getInstance().getTimeZone();
                ContentValues values = new ContentValues();
                values.put(CalendarContract.Events.DTSTART, TimeUnit.SECONDS.toMillis(event.startAt));
                values.put(CalendarContract.Events.DTEND, TimeUnit.SECONDS.toMillis(event.endAt));
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
                assert insertToCalendarURI != null;
                calendarEventID = Long.parseLong(insertToCalendarURI.getLastPathSegment());
                result = addEventToCalendarLink(event, calendarEventID);
            }
        } else if (method == EventToCalendarLoader.REMOVE) {
            result = getEventToCalendarLinkByEventId(eventId);
            if (result != null) {
                calendarEventID = result.getCalendarEventID();
                Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null, null, null);
                removeEventToCalendarLinkByEventId(eventId);
                assert cursor != null;
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
        long currentTime = getTimeStampInSeconds();
        ArrayList<EventAdvert> eventAdvertsSource = mEventAdvertDataSource.getAllEventAdverts();
        ArrayList<EventAdvert> eventAdverts = new ArrayList<>();
        EventAdvert eventAdvert;
        EventAdvert result = null;
        int i = 0;
        final int length = eventAdvertsSource.size();
        for (; i < length; i++) {
            eventAdvert = eventAdvertsSource.get(i);
            if (eventAdvert.eventId != eventId && checkOnEventAvailableInOffline(eventAdvert.eventId) && currentTime >= eventAdvert.advertStartAt && currentTime < (eventAdvert.advertStartAt == eventAdvert.advertEndAt ? eventAdvert.eventEndAt : eventAdvert.advertEndAt)) {
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
        //for debug
        /*if (result == null && eventAdvertsSource.size() > 0) {
            result = eventAdvertsSource.get(0);
        }*/
        return result;
    }

    private boolean checkOnEventAvailableInOffline(int eventId) {
        return NowApplication.getInstance().getState() != OFFLINE || mEventDataSource.getByEventId(eventId) != null;
    }

    private class EventNameComparator implements Comparator<Event> {
        public int compare(@NonNull Event left, @NonNull Event right) {
            return Long.valueOf(left.startAt).compareTo(right.startAt);
        }
    }
}
