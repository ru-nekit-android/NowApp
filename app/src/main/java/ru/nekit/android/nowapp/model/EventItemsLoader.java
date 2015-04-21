package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

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
import java.util.ArrayList;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.model.db.EventLocalDataSource;

/**
 * Created by chuvac on 13.03.15.
 */
public class EventItemsLoader extends AsyncTaskLoader<Integer> {

    public static final int RESULT_OK = 0;

    private static final String TAG_EVENTS = "events";
    private static final String SITE_NAME = "nowapp.ru";
    private static final String API_ROOT = "api/events.get";

    private DefaultHttpClient httpClient = new DefaultHttpClient();
    private Bundle mArgs;

    public EventItemsLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Integer loadInBackground() {

        Integer result = RESULT_OK;

        Context context = getContext();
        EventItemsModel eventModel = NowApplication.getEventModel();
        EventLocalDataSource localDataSource = eventModel.getLocalDataSource();
        if (NowApplication.getState() == NowApplication.STATE.ONLINE) {
            localDataSource.openForWrite();
        } else {
            localDataSource.openForRead();
        }

        int eventsCount = 0;
        ArrayList<EventItem> eventItems = new ArrayList<>();

        if (NowApplication.getState() == NowApplication.STATE.ONLINE) {
            Uri.Builder uriBuilder = new Uri.Builder()
                    .scheme("http")
                    .authority(SITE_NAME)
                    .path(API_ROOT);

            String type = null;
            if (mArgs != null) {
                type = mArgs.getString(EventItemsModel.LOADING_TYPE);
            }
            if (EventItemsModel.REFRESH_EVENT_ITEMS.equals(type) || type == null) {
                uriBuilder
                        .appendQueryParameter("fl", "1")
                        .appendQueryParameter("date", String.format("%d", EventItemsModel.getCurrentDateTimestamp(context, true)))
                        .appendQueryParameter("startAt", String.format("%d", EventItemsModel.getCurrentTimeTimestamp(context, true)));
            } else {
                EventItem lastEventItem = eventModel.getLastEvent();
                if (lastEventItem != null) {
                    String lastEventItemId = String.format("%d", lastEventItem.id);
                    uriBuilder
                            .appendQueryParameter("fl", "0")
                            .appendQueryParameter("date", String.format("%d", lastEventItem.date))
                            .appendQueryParameter("startAt", String.format("%d", lastEventItem.startAt))
                            .appendQueryParameter("id", lastEventItemId);
                }
            }

            Uri uri = uriBuilder.build();
            String query = uri.toString();

            try {

                HttpGet httpGet = new HttpGet(query);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                String jsonString = EntityUtils.toString(httpEntity);

                try {
                    JSONObject jsonRootObject = new JSONObject(jsonString);

                    String response = (String) jsonRootObject.get("response");
                    eventsCount = (int) jsonRootObject.get("events_count");

                    if ("ok".equals(response)) {
                        JSONArray eventJsonArray = jsonRootObject.getJSONArray(TAG_EVENTS);
                        eventModel.setReachEndOfDataList(eventJsonArray.length() == 0);
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

                            localDataSource.createOrUpdateEvent(eventItem);

                            eventItems.add(eventItem);

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
            if (EventItemsModel.REQUEST_NEW_EVENT_ITEMS.equals(type)) {
                eventModel.addEvents(eventItems);
                if (eventItems.size() > 0) {
                    eventModel.incrementCurrentPage();
                }
            } else {
                eventModel.setEvents(eventItems);
                if (eventItems.size() > 0) {
                    eventModel.setDefaultCurrentPage();
                }
            }
        } else {
            eventItems = localDataSource.getAllEvents();
            eventModel.setEvents(eventItems);
            eventModel.sortByStartTime();
            eventsCount = eventItems.size();
        }

        eventModel.setAvailableEventCount(eventsCount);

        if (result == RESULT_OK) {
            NowApplication.updateDataTimestamp();
        }

        return result;
    }



}
