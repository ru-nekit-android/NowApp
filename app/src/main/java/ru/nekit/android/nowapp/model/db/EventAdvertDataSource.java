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
                    EventFieldNameDictionary.ADVERT.TEXT,
                    EventFieldNameDictionary.ADVERT.FLAYER,
                    EventFieldNameDictionary.ADVERT.EVENT_START_AT,
                    EventFieldNameDictionary.ADVERT.EVENT_END_AT,
                    EventFieldNameDictionary.ADVERT.ADVERT_START_AT,
                    EventFieldNameDictionary.ADVERT.ADVERT_END_AT,
                    EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW,
                    EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH,
                    EventFieldNameDictionary.ADVERT.POSTER_THUMB,
                    EventFieldNameDictionary.ADVERT.POSTER_BLUR,
                    EventFieldNameDictionary.ADVERT.POSTER_ORIGINAL,
                    EventFieldNameDictionary.ADVERT.LOGO_THUMB,
                    EventFieldNameDictionary.ADVERT.LOGO_ORIGINAL
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
        contentValues.put(EventFieldNameDictionary.ADVERT.EVENT_START_AT, eventAdvert.eventStartAt);
        contentValues.put(EventFieldNameDictionary.ADVERT.EVENT_END_AT, eventAdvert.eventEndAt);
        contentValues.put(EventFieldNameDictionary.ADVERT.ADVERT_START_AT, eventAdvert.advertStartAt);
        contentValues.put(EventFieldNameDictionary.ADVERT.ADVERT_END_AT, eventAdvert.advertStartAt);
        contentValues.put(EventFieldNameDictionary.ADVERT.LOGO_THUMB, eventAdvert.logoThumb);
        contentValues.put(EventFieldNameDictionary.ADVERT.LOGO_ORIGINAL, eventAdvert.logoOriginal);
        contentValues.put(EventFieldNameDictionary.ADVERT.POSTER_BLUR, eventAdvert.posterBlur);
        contentValues.put(EventFieldNameDictionary.ADVERT.POSTER_ORIGINAL, eventAdvert.posterOriginal);
        contentValues.put(EventFieldNameDictionary.ADVERT.POSTER_THUMB, eventAdvert.posterThumb);
        contentValues.put(EventFieldNameDictionary.ADVERT.TEXT, eventAdvert.text);
        contentValues.put(EventFieldNameDictionary.ADVERT.FLAYER, eventAdvert.flayer);
        contentValues.put(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW, eventAdvert.showChanceLow);
        contentValues.put(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH, eventAdvert.showChanceHigh);
        database.insertWithOnConflict(EventSQLiteHelper.EVENT_ADVERT_TABLE_NAME, EventSQLiteHelper.EVENT_ADVERT_ID, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Nullable
    public EventAdvert getByEventId(int id) {
        EventAdvert eventAdvert = null;
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_ADVERT_TABLE_NAME,
                ALL_COLUMNS, EventSQLiteHelper.EVENT_ADVERT_ID + "=" + id, null, null, null, null);
        assert cursor != null;
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
        assert cursor != null;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            eventAdverts.add(cursorToEventAdvert(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return eventAdverts;
    }

    @NonNull
    private EventAdvert cursorToEventAdvert(@NonNull Cursor cursor) {
        EventAdvert eventAdvert = new EventAdvert();
        eventAdvert.id = cursor.getInt(cursor.getColumnIndex(EventSQLiteHelper.EVENT_ADVERT_ID));
        eventAdvert.eventId = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.EVENT_ID));
        eventAdvert.eventEndAt = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.EVENT_END_AT));
        eventAdvert.eventStartAt = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.EVENT_START_AT));
        eventAdvert.advertEndAt = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.ADVERT_END_AT));
        eventAdvert.advertStartAt = cursor.getLong(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.ADVERT_START_AT));
        eventAdvert.logoThumb = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.LOGO_THUMB));
        eventAdvert.logoOriginal = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.LOGO_ORIGINAL));
        eventAdvert.posterThumb = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.POSTER_THUMB));
        eventAdvert.posterOriginal = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.POSTER_ORIGINAL));
        eventAdvert.posterBlur = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.POSTER_BLUR));
        eventAdvert.flayer = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.FLAYER));
        eventAdvert.text = cursor.getString(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.TEXT));
        eventAdvert.showChanceLow = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW));
        eventAdvert.showChanceHigh = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH));

        return eventAdvert;
    }

    public void clear() {
        database.delete(EventSQLiteHelper.EVENT_ADVERT_TABLE_NAME, null, null);
    }
}
