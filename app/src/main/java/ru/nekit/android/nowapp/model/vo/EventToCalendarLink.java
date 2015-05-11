package ru.nekit.android.nowapp.model.vo;

import android.support.v4.util.Pair;

/**
 * Created by chuvac on 29.04.15.
 */
public class EventToCalendarLink extends Pair<Long, Long> {


    public EventToCalendarLink(Long eventID, Long CalendarEventID) {
        super(eventID, CalendarEventID);
    }

    public long getEventID() {
        return first;
    }

    public long getCalendarEventID() {
        return second;
    }
}
