package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.model.EventsModel;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventDataSource {

    private static final String[] ALL_COLUMNS =
            {
                    EventSQLiteHelper._ID,
                    EventFieldNameDictionary.ADDRESS,
                    EventFieldNameDictionary.ALL_NIGHT_PARTY,
                    EventFieldNameDictionary.DATE,
                    EventFieldNameDictionary.EMAIL,
                    EventFieldNameDictionary.END_AT,
                    EventFieldNameDictionary.START_AT,
                    EventFieldNameDictionary.ENTRANCE,
                    EventFieldNameDictionary.EVENT_CATEGORY,
                    EventFieldNameDictionary.NAME,
                    EventFieldNameDictionary.EVENT_DESCRIPTION,
                    EventFieldNameDictionary.LOGO_ORIGINAL,
                    EventFieldNameDictionary.LOGO_THUMB,
                    EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE,
                    EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE,
                    EventFieldNameDictionary.PHONE,
                    EventFieldNameDictionary.SITE,
                    EventFieldNameDictionary.PLACE_ID,
                    EventFieldNameDictionary.PLACE_NAME,
                    EventFieldNameDictionary.POSTER_BLUR,
                    EventFieldNameDictionary.POSTER_ORIGINAL,
                    EventFieldNameDictionary.POSTER_THUMB,
            };
    private static String[] FTS_SEARCH_ORDER = {
            EventSQLiteHelper.FTS_EVENT_CATEGORY_KEYWORD,
            EventSQLiteHelper.FTS_EVENT_START_TIME_ALIAS,
            EventFieldNameDictionary.NAME,
            EventFieldNameDictionary.EVENT_DESCRIPTION,
            EventFieldNameDictionary.PLACE_NAME,
            EventFieldNameDictionary.ADDRESS
    };
    private SQLiteDatabase database;
    private EventSQLiteHelper eventSQLHelper;
    private Context mContext;

    public EventDataSource(Context context, String dataBaseName, int databaseVersion) {
        eventSQLHelper = EventSQLiteHelper.getInstance(context, dataBaseName, databaseVersion);
        mContext = context;
    }

    public void openForWrite() throws SQLException {
        database = eventSQLHelper.getWritableDatabase();
    }

    public void createOrUpdateEvent(Event event) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EventSQLiteHelper._ID, event.id);
        contentValues.put(EventFieldNameDictionary.ADDRESS, event.address);
        contentValues.put(EventFieldNameDictionary.ALL_NIGHT_PARTY, event.allNightParty);
        contentValues.put(EventFieldNameDictionary.DATE, event.date);
        contentValues.put(EventFieldNameDictionary.EMAIL, event.email);
        contentValues.put(EventFieldNameDictionary.END_AT, event.endAt);
        contentValues.put(EventFieldNameDictionary.START_AT, event.startAt);
        contentValues.put(EventFieldNameDictionary.ENTRANCE, event.entrance);
        contentValues.put(EventFieldNameDictionary.EVENT_CATEGORY, event.category);
        contentValues.put(EventFieldNameDictionary.NAME, event.name);
        contentValues.put(EventFieldNameDictionary.EVENT_DESCRIPTION, event.eventDescription);
        contentValues.put(EventFieldNameDictionary.LOGO_ORIGINAL, event.logoOriginal);
        contentValues.put(EventFieldNameDictionary.LOGO_THUMB, event.logoThumb);
        contentValues.put(EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE, event.lat);
        contentValues.put(EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE, event.lng);
        contentValues.put(EventFieldNameDictionary.PHONE, event.phone);
        contentValues.put(EventFieldNameDictionary.SITE, event.site);
        contentValues.put(EventFieldNameDictionary.PLACE_ID, event.placeId);
        contentValues.put(EventFieldNameDictionary.PLACE_NAME, event.placeName);
        contentValues.put(EventFieldNameDictionary.POSTER_BLUR, event.posterBlur);
        contentValues.put(EventFieldNameDictionary.POSTER_ORIGINAL, event.posterOriginal);
        contentValues.put(EventFieldNameDictionary.POSTER_THUMB, event.posterThumb);
        database.insertWithOnConflict(EventSQLiteHelper.EVENT_TABLE_NAME, EventSQLiteHelper._ID, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        ContentValues contentValuesFTS = new ContentValues();
        contentValuesFTS.put(EventSQLiteHelper._ID, event.id);
        contentValuesFTS.put(EventFieldNameDictionary.NAME, normalizeForSearch(event.name));
        contentValuesFTS.put(EventFieldNameDictionary.EVENT_DESCRIPTION, normalizeForSearch(event.eventDescription));
        contentValuesFTS.put(EventFieldNameDictionary.PLACE_NAME, normalizeForSearch(event.placeName));
        contentValuesFTS.put(EventFieldNameDictionary.ADDRESS, normalizeForSearch(event.address));
        contentValuesFTS.put(EventSQLiteHelper.FTS_EVENT_START_TIME_ALIAS, EventsModel.getStartTimeKeywords(mContext, event).toLowerCase());
        contentValuesFTS.put(EventSQLiteHelper.FTS_EVENT_CATEGORY_KEYWORD, EventsModel.getCategoryKeywords(event.category).toLowerCase());
        contentValuesFTS.put(EventSQLiteHelper.FTS_EVENT_START_TIME, event.startAt + event.date);
        database.insert(EventSQLiteHelper.FTS_TABLE_NAME, null, contentValuesFTS);
    }

    private String normalizeForSearch(String value) {
        return value.toLowerCase().replaceAll("\"|«|\\(", "");
    }

    public ArrayList<Event> getAllEvents() {
        ArrayList<Event> eventList = new ArrayList<>();
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_TABLE_NAME,
                ALL_COLUMNS, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Event event = cursorToEventItem(cursor);
            eventList.add(event);
            cursor.moveToNext();
        }
        cursor.close();
        return eventList;
    }

    public ArrayList<Event> getByEventIds(ArrayList<Integer> ids) {
        ArrayList<Event> events = new ArrayList<>();
        if (ids.size() > 0) {
            Cursor cursor = database.query(EventSQLiteHelper.EVENT_TABLE_NAME,
                    ALL_COLUMNS, EventSQLiteHelper._ID + " IN (" + TextUtils.join(",", ids) + ")", null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Event event = cursorToEventItem(cursor);
                events.add(event);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return events;
    }

    public Event getByEventId(int id) {
        Event event = null;
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_TABLE_NAME,
                ALL_COLUMNS, EventSQLiteHelper._ID + "=" + id, null, null, null, null);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            event = cursorToEventItem(cursor);
        }
        cursor.close();
        return event;
    }

    private Event cursorToEventItem(Cursor cursor) {
        Event event = new Event();
        event.id = cursor.getInt(cursor.getColumnIndex(EventSQLiteHelper._ID));
        event.address = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADDRESS));
        event.allNightParty = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ALL_NIGHT_PARTY));
        event.date = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.DATE));
        event.email = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.EMAIL));
        event.endAt = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.END_AT));
        event.startAt = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.START_AT));
        event.entrance = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ENTRANCE));
        event.category = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_CATEGORY));
        event.name = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.NAME));
        event.eventDescription = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_DESCRIPTION));
        event.logoOriginal = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.LOGO_ORIGINAL));
        event.logoThumb = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.LOGO_THUMB));
        event.lat = cursor.getFloat(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE));
        event.lng = cursor.getFloat(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE));
        event.phone = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.PHONE));
        event.site = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.SITE));
        event.placeId = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.PLACE_ID));
        event.placeName = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.PLACE_NAME));
        event.posterBlur = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.POSTER_BLUR));
        event.posterOriginal = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.POSTER_ORIGINAL));
        event.posterThumb = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.POSTER_THUMB));
        return event;
    }

    public ArrayList<Event> fullTextSearch(@NonNull String query) {
        query = normalizeForSearch(query);
        ArrayList<Event> eventList = new ArrayList<>();
        ArrayList<String> queryList = new ArrayList<>();
        for (String field : FTS_SEARCH_ORDER) {
            queryList.add(EventSQLiteHelper._ID + " IN (" +
                    "SELECT " + EventSQLiteHelper._ID + " FROM " + EventSQLiteHelper.FTS_TABLE_NAME + " WHERE " + field + " MATCH '" + query + "') ");
        }
        Cursor cursor = database.rawQuery("SELECT * FROM " + EventSQLiteHelper.EVENT_TABLE_NAME + " WHERE " + TextUtils.join(" OR ", queryList) + ";", null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                eventList.add(cursorToEventItem(cursor));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return eventList;
    }

    public void close() {
        eventSQLHelper.close();
    }

    public void removeEventById(int id) {
        database.delete(EventSQLiteHelper.EVENT_TABLE_NAME, String.format("%s = %s", EventSQLiteHelper._ID, id), null);
        database.delete(EventSQLiteHelper.FTS_TABLE_NAME, String.format("%s = %s", EventSQLiteHelper._ID, id), null);
    }

    public void clear() {
        database.delete(EventSQLiteHelper.EVENT_TABLE_NAME, null, null);
        database.delete(EventSQLiteHelper.FTS_TABLE_NAME, null, null);
    }
}
