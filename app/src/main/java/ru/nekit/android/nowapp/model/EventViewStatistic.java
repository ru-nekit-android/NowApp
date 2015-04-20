package ru.nekit.android.nowapp.model;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Created by chuvac on 20.04.15.
 */
@ParseClassName("EventViewStatistic")
public class EventViewStatistic extends ParseObject {

    public void setEventId(int eventId) {
        put("eventId", eventId);
    }

    public void setDeviceId(String deviceId) {
        put("deviceId", deviceId);
    }

    public static ParseQuery<EventViewStatistic> getUniqueViewStatisticForDeviceQuery(int eventId, String deviceId) {
        return getQuery().whereEqualTo("eventId", eventId).whereEqualTo("deviceId", deviceId);
    }

    public static ParseQuery<EventViewStatistic> getUniqueViewStatisticForEventQuery(int eventId) {
        return getQuery().whereEqualTo("eventId", eventId);
    }

    public static ParseQuery<EventViewStatistic> getQuery() {
        return ParseQuery.getQuery(EventViewStatistic.class);
    }


}
