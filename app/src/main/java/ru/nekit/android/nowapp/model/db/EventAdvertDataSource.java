package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;
import ru.nekit.android.nowapp.model.vo.EventAdvert;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventAdvertDataSource {

    private static final String[] ALL_COLUMNS =
            {
                    EventSQLiteHelper.EVENT_ADVERT_ID,
                    EventFieldNameDictionary.ADVERT.EVENT_ID,
                    EventFieldNameDictionary.ADVERT.START_AT,
                    EventFieldNameDictionary.ADVERT.LOGO_THUMB,
                    EventFieldNameDictionary.ADVERT.LINK,
                    EventFieldNameDictionary.ADVERT.NAME,
                    EventFieldNameDictionary.ADVERT.PLACE_NAME,
                    EventFieldNameDictionary.ADVERT.AD_TYPE,
                    EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW,
                    EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH,
            };

    private SQLiteDatabase database;
    private EventSQLiteHelper eventSQLHelper;

    public EventAdvertDataSource(Context context, String dataBaseName, int databaseVersion) {
        eventSQLHelper = EventSQLiteHelper.getInstance(context, dataBaseName, databaseVersion);
    }

    public void openForWrite() throws SQLException {
        database = eventSQLHelper.getWritableDatabase();
    }

    public void createEventAdvert(@NonNull EventAdvert eventAdvert) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EventSQLiteHelper.EVENT_ADVERT_ID, eventAdvert.id);
        contentValues.put(EventFieldNameDictionary.ADVERT.EVENT_ID, eventAdvert.eventId);
        contentValues.put(EventFieldNameDictionary.ADVERT.START_AT, eventAdvert.startAt);
        contentValues.put(EventFieldNameDictionary.ADVERT.LOGO_THUMB, eventAdvert.logoThumb);
        contentValues.put(EventFieldNameDictionary.ADVERT.LINK, eventAdvert.link);
        contentValues.put(EventFieldNameDictionary.ADVERT.NAME, eventAdvert.name);
        contentValues.put(EventFieldNameDictionary.ADVERT.PLACE_NAME, eventAdvert.placeName);
        contentValues.put(EventFieldNameDictionary.ADVERT.AD_TYPE, eventAdvert.adType);
        contentValues.put(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW, eventAdvert.showChanceLow);
        contentValues.put(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH, eventAdvert.showChanceHigh);
        database.insertWithOnConflict(EventSQLiteHelper.EVENT_ADVERT_TABLE_NAME, EventSQLiteHelper.EVENT_ADVERT_ID, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Nullable
    public EventAdvert getByEventId(int id) {
        EventAdvert eventAdvert = null;
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_ADVERT_TABLE_NAME,
                ALL_COLUMNS, EventSQLiteHelper.EVENT_ADVERT_ID + "=" + id, null, null, null, null);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            eventAdvert = cursorToEventAdvert(cursor);
        }
        cursor.close();


        return eventAdvert;
    }

    @NonNull
    public ArrayList<EventAdvert> getAllEventAdverts() {
        ArrayList<EventAdvert> eventAdverts = new ArrayList<>();
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_ADVERT_TABLE_NAME,
                ALL_COLUMNS, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                eventAdverts.add(cursorToEventAdvert(cursor));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return eventAdverts;
    }

    @NonNull
    private EventAdvert cursorToEventAdvert(@NonNull Cursor cursor) {
        EventAdvert eventAdvert = new EventAdvert();
        eventAdvert.id = cursor.getInt(cursor.getColumnIndex(EventSQLiteHelper.EVENT_ADVERT_ID));
        eventAdvert.eventId = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.EVENT_ID));
        eventAdvert.startAt = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.START_AT));
        eventAdvert.logoThumb = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.LOGO_THUMB));
        eventAdvert.link = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.LINK));
        eventAdvert.name = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.NAME));
        eventAdvert.placeName = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.PLACE_NAME));
        eventAdvert.adType = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.AD_TYPE));
        eventAdvert.showChanceLow = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW));
        eventAdvert.showChanceHigh = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH));
        return eventAdvert;
    }

    public void clear() {
        database.delete(EventSQLiteHelper.EVENT_ADVERT_TABLE_NAME, null, null);
    }
}
