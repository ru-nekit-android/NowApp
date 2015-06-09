package ru.nekit.android.nowapp.model.vo;

/**
 * Created by chuvac on 29.04.15.
 */
public class EventToCalendarLink {

    private long eventId;
    private long calendarEventId;

    public EventToCalendarLink(long eventId, long calendarEventId) {
        this.eventId = eventId;
        this.calendarEventId = calendarEventId;
    }

    public long getEventID() {
        return eventId;
    }

    public long getCalendarEventID() {
        return calendarEventId;
    }
}
