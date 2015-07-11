package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventToCalendarDataSource {

    private SQLiteDatabase database;
    private EventSQLiteHelper dbHelper;

    private static final String[] ALL_COLUMNS =
            {
                    EventSQLiteHelper.EVENT_ID,
                    EventSQLiteHelper.CALENDAR_EVENT_ID
            };

    public EventToCalendarDataSource(Context context, String dataBaseName, int databaseVersion) {
        dbHelper = EventSQLiteHelper.getInstance(context, dataBaseName, databaseVersion);
    }

    public void openForWrite() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void openForRead() throws SQLException {
        database = dbHelper.getReadableDatabase();
    }

    @Nullable
    public EventToCalendarLink addLink(long eventID, long calendarID) {
        ContentValues values = new ContentValues();
        values.put(EventSQLiteHelper.EVENT_ID, eventID);
        values.put(EventSQLiteHelper.CALENDAR_EVENT_ID, calendarID);
        long result = database.insert(EventSQLiteHelper.EVENT_TO_CALENDAR_LINK_TABLE_NAME, null, values);
        if (result != -1) {
            return new EventToCalendarLink(eventID, calendarID);
        }
        return null;
    }

    @NonNull
    public ArrayList<EventToCalendarLink> getAllEventToCalendarLinks() {
        ArrayList<EventToCalendarLink> linkList = new ArrayList<>();

        Cursor cursor = database.query(EventSQLiteHelper.EVENT_TO_CALENDAR_LINK_TABLE_NAME,
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

    @NonNull
    private EventToCalendarLink cursorToLink(@NonNull Cursor cursor) {
        return new EventToCalendarLink(cursor.getLong(cursor.getColumnIndex(EventSQLiteHelper.EVENT_ID)), cursor.getLong(cursor.getColumnIndex(EventSQLiteHelper.CALENDAR_EVENT_ID)));
    }

    public void close() {
        dbHelper.close();
    }

    public void removeLinkByEventId(long id) {
        String idString = String.valueOf(id);
        database.delete(EventSQLiteHelper.EVENT_TABLE_NAME, String.format("%s = %s", EventSQLiteHelper.EVENT_ID, idString), null);
    }

    @Nullable
    public EventToCalendarLink getLinkByEventID(long ID) {
        String idString = String.valueOf(ID);
        EventToCalendarLink link = null;
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_TO_CALENDAR_LINK_TABLE_NAME, ALL_COLUMNS, String.format("%s = %s;", EventSQLiteHelper.EVENT_ID, idString), null, null, null, null);
        if (cursor.moveToFirst()) {
            link = cursorToLink(cursor);
        }
        cursor.close();
        return link;
    }
}
