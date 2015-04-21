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
        EventItemsModel model = ((NowApplication) context).getEventModel();
        ArrayList<EventItem> eventItems = new ArrayList<>();
        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme("http")
                .authority(SITE_NAME)
                .path(API_ROOT);

        String type = null;
        if (mArgs != null) {
            type = mArgs.getString(EventItemsModel.TYPE);
        }
        if (EventItemsModel.REFRESH_EVENT_ITEMS.equals(type) || type == null) {
            uriBuilder
                    .appendQueryParameter("fl", "1")
                    .appendQueryParameter("date", String.format("%d", EventItemsModel.getCurrentDateTimestamp(context, true)))
                    .appendQueryParameter("startAt", String.format("%d", EventItemsModel.getCurrentTimeTimestamp(context, true)));
        } else {
            EventItem lastEventItem = model.getLastEvent();
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
        int eventsCount = 0;

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
                    model.setReachEndOfDataList(eventJsonArray.length() == 0);
                    for (int i = 0; i < eventJsonArray.length(); i++) {
                        JSONObject jsonEventItem = eventJsonArray.getJSONObject(i);

                        EventItem eventItem = new EventItem();
                        eventItem.date = jsonEventItem.optLong(EventItemFieldNameDictionary.DATE, 0);
                        eventItem.eventDescription = jsonEventItem.optString(EventItemFieldNameDictionary.EVENT_DESCRIPTION);
                        eventItem.placeName = jsonEventItem.optString(EventItemFieldNameDictionary.PLACE_NAME);
                        eventItem.placeId = jsonEventItem.optInt(EventItemFieldNameDictionary.PLACE_ID);
                        eventItem.id = jsonEventItem.optInt(EventItemFieldNameDictionary.ID);
                        eventItem.category = jsonEventItem.optString(EventItemFieldNameDictionary.EVENT_CATEGORY);
                        eventItem.entrance = jsonEventItem.optString(EventItemFieldNameDictionary.ENTRANCE);
                        eventItem.address = jsonEventItem.optString(EventItemFieldNameDictionary.ADDRESS);
                        eventItem.phone = jsonEventItem.optString(EventItemFieldNameDictionary.PHONE);
                        eventItem.site = jsonEventItem.optString(EventItemFieldNameDictionary.SITE);
                        eventItem.email = jsonEventItem.optString(EventItemFieldNameDictionary.EMAIL);
                        eventItem.lat = jsonEventItem.optDouble(EventItemFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE);
                        eventItem.lng = jsonEventItem.optDouble(EventItemFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE);
                        eventItem.name = jsonEventItem.optString(EventItemFieldNameDictionary.NAME);
                        eventItem.startAt = jsonEventItem.optLong(EventItemFieldNameDictionary.START_AT);
                        eventItem.endAt = jsonEventItem.optLong(EventItemFieldNameDictionary.END_AT);
                        eventItem.posterThumb = jsonEventItem.optString(EventItemFieldNameDictionary.POSTER_THUMB);
                        eventItem.posterBlur = jsonEventItem.optString(EventItemFieldNameDictionary.POSTER_BLUR);
                        eventItem.posterOriginal = jsonEventItem.optString(EventItemFieldNameDictionary.POSTER_ORIGINAL);
                        eventItem.logoOriginal = jsonEventItem.optString(EventItemFieldNameDictionary.LOGO_ORIGINAL);
                        eventItem.logoThumb = jsonEventItem.optString(EventItemFieldNameDictionary.LOGO_THUMB);
                        eventItem.allNightParty = jsonEventItem.optBoolean(EventItemFieldNameDictionary.ALL_NIGHT_PARTY) ? 1 : 0;

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
            model.addEvents(eventItems);
            if (eventItems.size() > 0) {
                model.incrementCurrentPage();
            }
        } else {
            model.setEvents(eventItems);
            if (eventItems.size() > 0) {
                model.setDefaultCurrentPage();
            }
        }

        model.setAvailableEventCount(eventsCount);

        return result;
    }

}
