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

import ru.nekit.android.nowapp.model.db.vo.EventToCalendarLink;

/**
 * Created by chuvac on 30.04.15.
 */
public class EventToCalendarLoader extends AsyncTaskLoader<EventToCalendarLink> {

    public static final String EVENT_ITEM_KEY = "event_item_id_key";
    public static final String METHOD_KEY = "method_key";
    public static final int CHECK = 1;
    public static final int ADD = 2;

    private final int mMethod;
    private final EventItem mEventItem;

    public EventToCalendarLoader(Context context, Bundle args) {
        super(context);
        mMethod = args.getInt(METHOD_KEY);
        mEventItem = args.getParcelable(EVENT_ITEM_KEY);
    }

    @Override
    public EventToCalendarLink loadInBackground() {
        EventToCalendarLink result = null;
        long calendarEventID;
        Context context = getContext();
        EventItemsModel model = EventItemsModel.getInstance(context);
        model.getEventToCalendarLinker().openForWrite();
        ContentResolver contentResolver = context.getContentResolver();
        if (mMethod == CHECK) {
            result = model.getEventToCalendarLink(mEventItem);
            if (result != null) {
                calendarEventID = result.getCalendarEventID();
                Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventID), null, null, null, null);
                if (!cursor.moveToFirst()) {
                    result = null;
                    model.removeEventToCalendarLink(mEventItem);
                }
                cursor.close();
            }
        } else if (mMethod == ADD) {
            TimeZone timeZone = Calendar.getInstance().getTimeZone();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, TimeUnit.SECONDS.toMillis(EventItemsModel.getEventStartTimeInSeconds(mEventItem)));
            values.put(CalendarContract.Events.DTEND, TimeUnit.SECONDS.toMillis(EventItemsModel.getEventEndTimeInSeconds(mEventItem)));
            values.put(CalendarContract.Events.TITLE, mEventItem.name);
            values.put(CalendarContract.Events.EVENT_LOCATION, mEventItem.placeName);
            values.put(CalendarContract.Events.DESCRIPTION, mEventItem.eventDescription);
            values.put(CalendarContract.Events.EVENT_COLOR, EventItemsModel.getCategoryColor(mEventItem.category));
            values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
            values.put(CalendarContract.Events.CALENDAR_ID, 1);
            Uri insertToCalendarURI = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
            calendarEventID = Long.parseLong(insertToCalendarURI.getLastPathSegment());
            result = model.addEventToCalendarLink(mEventItem, calendarEventID);
        }
        return result;
    }
}