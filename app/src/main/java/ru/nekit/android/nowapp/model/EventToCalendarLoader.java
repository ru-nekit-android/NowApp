package ru.nekit.android.nowapp.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.content.AsyncTaskLoader;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;

/**
 * Created by chuvac on 30.04.15.
 */
public class EventToCalendarLoader extends AsyncTaskLoader<EventToCalendarLink> {

    public static final String EVENT_ITEM_ID_KEY = "event_item_id_key";
    public static final String METHOD_KEY = "method_key";
    public static final int CHECK = 1;
    public static final int ADD = 2;

    private final int mMethod;
    private final int mEventItemId;

    public EventToCalendarLoader(Context context, Bundle args) {
        super(context);
        mMethod = args.getInt(METHOD_KEY);
        mEventItemId = args.getInt(EVENT_ITEM_ID_KEY);
    }

    @Override
    public EventToCalendarLink loadInBackground() {
        EventToCalendarLink result = null;
        long calendarEventID;
        Context context = getContext().getApplicationContext();
        EventItemsModel model = NowApplication.getEventModel();
        model.openEventToCalendarLinker(true);
        ContentResolver contentResolver = context.getContentResolver();
        if (mMethod == CHECK) {
            result = model.getEventToCalendarLinkByEventId(mEventItemId);
            if (result != null) {
                calendarEventID = result.getCalendarEventID();
                Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null, null, null);
                if (!cursor.moveToFirst()) {
                    result = null;
                    model.removeEventToCalendarLinkByEventId(mEventItemId);
                }
                cursor.close();
            }
        } else if (mMethod == ADD) {
            EventItem eventItem = model.getEventItemByID(mEventItemId);
            TimeZone timeZone = Calendar.getInstance().getTimeZone();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, TimeUnit.SECONDS.toMillis(EventItemsModel.getEventStartTimeInSeconds(eventItem)));
            values.put(CalendarContract.Events.DTEND, TimeUnit.SECONDS.toMillis(EventItemsModel.getEventEndTimeInSeconds(eventItem)));
            values.put(CalendarContract.Events.TITLE, eventItem.name);
            values.put(CalendarContract.Events.EVENT_LOCATION, eventItem.placeName);
            if (!"".equals(eventItem.email)) {
                values.put(CalendarContract.Events.ORGANIZER, eventItem.email);
            }
            values.put(CalendarContract.Events.DESCRIPTION, eventItem.eventDescription);
            values.put(CalendarContract.Events.EVENT_COLOR, EventItemsModel.getCategoryColor(eventItem.category));
            values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
            values.put(CalendarContract.Events.CALENDAR_ID, 1);
            Uri insertToCalendarURI = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
            calendarEventID = Long.parseLong(insertToCalendarURI.getLastPathSegment());
            result = model.addEventToCalendarLink(eventItem, calendarEventID);
        }
        return result;
    }
}