package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;
import ru.nekit.android.nowapp.model.EventItem;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventLocalDataSource {

    private SQLiteDatabase database;
    private EventSQLiteHelper eventSQLHelper;

    private static final String[] ALL_COLUMNS =
            {
                    EventFieldNameDictionary.ID,
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
                    EventFieldNameDictionary.POSTER_THUMB
            };

    public EventLocalDataSource(Context context, String dataBaseName, int databaseVersion) {
        eventSQLHelper = EventSQLiteHelper.getInstance(context, dataBaseName, databaseVersion);
    }

    public void openForWrite() throws SQLException {
        database = eventSQLHelper.getWritableDatabase();
    }

    public void openForRead() throws SQLException {
        database = eventSQLHelper.getReadableDatabase();
    }

    public void createOrUpdateEvent(EventItem eventItem) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EventFieldNameDictionary.ID, eventItem.id);
        contentValues.put(EventFieldNameDictionary.ADDRESS, eventItem.address);
        contentValues.put(EventFieldNameDictionary.ALL_NIGHT_PARTY, eventItem.allNightParty);
        contentValues.put(EventFieldNameDictionary.DATE, eventItem.date);
        contentValues.put(EventFieldNameDictionary.EMAIL, eventItem.email);
        contentValues.put(EventFieldNameDictionary.END_AT, eventItem.endAt);
        contentValues.put(EventFieldNameDictionary.START_AT, eventItem.startAt);
        contentValues.put(EventFieldNameDictionary.ENTRANCE, eventItem.entrance);
        contentValues.put(EventFieldNameDictionary.EVENT_CATEGORY, eventItem.category);
        contentValues.put(EventFieldNameDictionary.NAME, eventItem.name);
        contentValues.put(EventFieldNameDictionary.EVENT_DESCRIPTION, eventItem.eventDescription);
        contentValues.put(EventFieldNameDictionary.LOGO_ORIGINAL, eventItem.logoOriginal);
        contentValues.put(EventFieldNameDictionary.LOGO_THUMB, eventItem.logoThumb);
        contentValues.put(EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE, eventItem.lat);
        contentValues.put(EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE, eventItem.lng);
        contentValues.put(EventFieldNameDictionary.PHONE, eventItem.phone);
        contentValues.put(EventFieldNameDictionary.SITE, eventItem.site);
        contentValues.put(EventFieldNameDictionary.PLACE_ID, eventItem.placeId);
        contentValues.put(EventFieldNameDictionary.PLACE_NAME, eventItem.placeName);
        contentValues.put(EventFieldNameDictionary.POSTER_BLUR, eventItem.posterBlur);
        contentValues.put(EventFieldNameDictionary.POSTER_ORIGINAL, eventItem.posterOriginal);
        contentValues.put(EventFieldNameDictionary.POSTER_THUMB, eventItem.posterThumb);
        database.insertWithOnConflict(EventSQLiteHelper.TABLE_NAME, EventFieldNameDictionary.ID, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        ContentValues contentValuesFTS = new ContentValues();
        contentValuesFTS.put(EventFieldNameDictionary.ID, eventItem.id);
        contentValuesFTS.put(EventFieldNameDictionary.NAME, eventItem.name.toLowerCase());
        contentValuesFTS.put(EventFieldNameDictionary.EVENT_DESCRIPTION, eventItem.eventDescription.toLowerCase());
        contentValuesFTS.put(EventFieldNameDictionary.PLACE_NAME, eventItem.placeName.toLowerCase());
        contentValuesFTS.put(EventFieldNameDictionary.ADDRESS, eventItem.address.toLowerCase());
        long id = database.insert(EventSQLiteHelper.FTS_TABLE_NAME, null, contentValuesFTS);
    }

    public ArrayList<EventItem> getAllEvents() {
        ArrayList<EventItem> eventList = new ArrayList<>();
        Cursor cursor = database.query(EventSQLiteHelper.TABLE_NAME,
                ALL_COLUMNS, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            EventItem event = cursorToEventItem(cursor);
            eventList.add(event);
            cursor.moveToNext();
        }
        cursor.close();
        return eventList;
    }

    public ArrayList<EventItem> getByEventIDs(ArrayList<Integer> ids) {
        ArrayList<EventItem> eventItems = new ArrayList<>();
        if (ids.size() > 0) {
            Cursor cursor = database.query(EventSQLiteHelper.TABLE_NAME,
                    ALL_COLUMNS, EventFieldNameDictionary.ID + " IN (" + TextUtils.join(",", ids) + ")", null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                EventItem eventitem = cursorToEventItem(cursor);
                eventItems.add(eventitem);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return eventItems;
    }

    private EventItem cursorToEventItem(Cursor cursor) {
        EventItem eventItem = new EventItem();
        eventItem.id = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ID));
        eventItem.address = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADDRESS));
        eventItem.allNightParty = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ALL_NIGHT_PARTY));
        eventItem.date = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.DATE));
        eventItem.email = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.EMAIL));
        eventItem.endAt = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.END_AT));
        eventItem.startAt = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.START_AT));
        eventItem.entrance = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ENTRANCE));
        eventItem.category = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_CATEGORY));
        eventItem.name = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.NAME));
        eventItem.eventDescription = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_DESCRIPTION));
        eventItem.logoOriginal = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.LOGO_ORIGINAL));
        eventItem.logoThumb = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.LOGO_THUMB));
        eventItem.lat = cursor.getFloat(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE));
        eventItem.lng = cursor.getFloat(cursor.getColumnIndex(EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE));
        eventItem.phone = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.PHONE));
        eventItem.site = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.SITE));
        eventItem.placeId = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.PLACE_ID));
        eventItem.placeName = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.PLACE_NAME));
        eventItem.posterBlur = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.POSTER_BLUR));
        eventItem.posterOriginal = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.POSTER_ORIGINAL));
        eventItem.posterThumb = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.POSTER_THUMB));
        return eventItem;
    }

    public ArrayList<EventItem> searchByField(String field, String query) {
        ArrayList<EventItem> eventList = new ArrayList<>();
        Cursor cursor = database.query(EventSQLiteHelper.TABLE_NAME, ALL_COLUMNS, field + " LIKE '%" + query + "%'", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            EventItem event = cursorToEventItem(cursor);
            eventList.add(event);
            cursor.moveToNext();
        }
        cursor.close();
        return eventList;
    }

    public ArrayList<Integer> fullTextSearchByField(@Nullable String field, @NonNull String query) {
        ArrayList<Integer> eventIDList = new ArrayList<>();
        Cursor cursor = database.query(EventSQLiteHelper.FTS_TABLE_NAME, new String[]{EventFieldNameDictionary.ID}, EventSQLiteHelper.FTS_TABLE_NAME + " MATCH '" + (field == null ? "" : field + ":") + query + "';", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            eventIDList.add(cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ID)));
            cursor.moveToNext();
        }
        cursor.close();
        return eventIDList;
    }

    public void close() {
        eventSQLHelper.close();
    }

    public void removeEventByID(int id) {
        database.delete(EventSQLiteHelper.TABLE_NAME, String.format("%s = %s", EventFieldNameDictionary.ID, id), null);
        database.delete(EventSQLiteHelper.FTS_TABLE_NAME, String.format("%s = %s", EventFieldNameDictionary.ID, id), null);
    }

    public void clear() {
        database.delete(EventSQLiteHelper.TABLE_NAME, null, null);
        database.delete(EventSQLiteHelper.FTS_TABLE_NAME, null, null);
    }
}
