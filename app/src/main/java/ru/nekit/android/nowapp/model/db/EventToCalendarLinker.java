package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import ru.nekit.android.nowapp.model.db.vo.EventToCalendarLink;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventToCalendarLinker {

    private SQLiteDatabase database;
    private EventToCalendarSQLiteHelper dbHelper;

    private static final String[] ALL_COLUMNS =
            {
                    EventToCalendarSQLiteHelper.EVENT_ID,
                    EventToCalendarSQLiteHelper.CALENDAR_EVENT_ID
            };

    public EventToCalendarLinker(Context context, String dataBaseName) {
        dbHelper = new EventToCalendarSQLiteHelper(context, dataBaseName);
    }

    public void openForWrite() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void openForRead() throws SQLException {
        database = dbHelper.getReadableDatabase();
    }

    public EventToCalendarLink addLink(long eventID, long calendarID) {
        ContentValues values = new ContentValues();
        values.put(EventToCalendarSQLiteHelper.EVENT_ID, eventID);
        values.put(EventToCalendarSQLiteHelper.CALENDAR_EVENT_ID, calendarID);
        return new EventToCalendarLink(eventID, database.insert(EventToCalendarSQLiteHelper.TABLE_NAME, null, values));
    }

    public ArrayList<EventToCalendarLink> getAllEventToCalendarLinks() {
        ArrayList<EventToCalendarLink> linkList = new ArrayList<>();

        Cursor cursor = database.query(EventToCalendarSQLiteHelper.TABLE_NAME,
                ALL_COLUMNS, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            EventToCalendarLink link = cursorToLink(cursor);
            linkList.add(link);
            cursor.moveToNext();
        }
        cursor.close();
        return linkList;
    }

    private EventToCalendarLink cursorToLink(Cursor cursor) {
        return new EventToCalendarLink(cursor.getLong(cursor.getColumnIndex(EventToCalendarSQLiteHelper.EVENT_ID)), cursor.getLong(cursor.getColumnIndex(EventToCalendarSQLiteHelper.CALENDAR_EVENT_ID)));
    }

    public void close() {
        dbHelper.close();
    }

    public void removeLinkByEventID(long ID) {
        String idString = String.valueOf(ID);
        database.delete(EventToCalendarSQLiteHelper.TABLE_NAME, String.format("%s = %s", EventToCalendarSQLiteHelper.EVENT_ID, idString), null);
    }

    public EventToCalendarLink getLinkByEventID(long ID) {
        String idString = String.valueOf(ID);
        EventToCalendarLink link = null;
        Cursor cursor = database.query(EventToCalendarSQLiteHelper.TABLE_NAME, ALL_COLUMNS, String.format("%s = %s;", EventToCalendarSQLiteHelper.EVENT_ID, idString), null, null, null, null);
        if (cursor.moveToFirst()) {
            link = cursorToLink(cursor);
        }
        cursor.close();
        return link;
    }
}
