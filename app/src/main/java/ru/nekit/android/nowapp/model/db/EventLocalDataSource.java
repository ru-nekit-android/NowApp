package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;
import ru.nekit.android.nowapp.model.EventItem;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventLocalDataSource {

    private SQLiteDatabase database;
    private EventSQLiteHelper dbHelper;

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

    public EventLocalDataSource(Context context, String dataBaseName) {
        dbHelper = new EventSQLiteHelper(context, dataBaseName);
    }

    public void openForWrite() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void openForRead() throws SQLException {
        database = dbHelper.getReadableDatabase();
    }

    public void createOrUpdateEvent(EventItem eventItem) {
        ContentValues values = new ContentValues();
        values.put(EventFieldNameDictionary.ID, eventItem.id);
        values.put(EventFieldNameDictionary.ADDRESS, eventItem.address);
        values.put(EventFieldNameDictionary.ALL_NIGHT_PARTY, eventItem.allNightParty);
        values.put(EventFieldNameDictionary.DATE, eventItem.date);
        values.put(EventFieldNameDictionary.EMAIL, eventItem.email);
        values.put(EventFieldNameDictionary.END_AT, eventItem.endAt);
        values.put(EventFieldNameDictionary.START_AT, eventItem.startAt);
        values.put(EventFieldNameDictionary.ENTRANCE, eventItem.entrance);
        values.put(EventFieldNameDictionary.EVENT_CATEGORY, eventItem.category);
        values.put(EventFieldNameDictionary.NAME, eventItem.name);
        values.put(EventFieldNameDictionary.EVENT_DESCRIPTION, eventItem.eventDescription);
        values.put(EventFieldNameDictionary.LOGO_ORIGINAL, eventItem.logoOriginal);
        values.put(EventFieldNameDictionary.LOGO_THUMB, eventItem.logoThumb);
        values.put(EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE, eventItem.lat);
        values.put(EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE, eventItem.lng);
        values.put(EventFieldNameDictionary.PHONE, eventItem.phone);
        values.put(EventFieldNameDictionary.SITE, eventItem.site);
        values.put(EventFieldNameDictionary.PLACE_ID, eventItem.placeId);
        values.put(EventFieldNameDictionary.PLACE_NAME, eventItem.placeName);
        values.put(EventFieldNameDictionary.POSTER_BLUR, eventItem.posterBlur);
        values.put(EventFieldNameDictionary.POSTER_ORIGINAL, eventItem.posterOriginal);
        values.put(EventFieldNameDictionary.POSTER_THUMB, eventItem.posterThumb);
        database.insertWithOnConflict(EventSQLiteHelper.TABLE_NAME, EventFieldNameDictionary.ID, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public ArrayList<EventItem> getAllEvents() {
        ArrayList<EventItem> eventItems = new ArrayList<EventItem>();

        Cursor cursor = database.query(EventSQLiteHelper.TABLE_NAME,
                ALL_COLUMNS, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            EventItem eventitem = cursorToEventItem(cursor);
            eventItems.add(eventitem);
            cursor.moveToNext();
        }
        cursor.close();
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


    public void close() {
        dbHelper.close();
    }

    public void removeEventByID(int ID) {
        String idString = String.valueOf(ID);
        database.delete(EventSQLiteHelper.TABLE_NAME, String.format("%s = %s", EventFieldNameDictionary.ID, idString), null);
    }
}
