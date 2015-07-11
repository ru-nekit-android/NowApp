package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;
import ru.nekit.android.nowapp.model.vo.EventStats;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventStatsDataSource {

    private static final String[] ALL_COLUMNS =
            {
                    EventSQLiteHelper.EVENT_ID,
                    EventFieldNameDictionary.LIKES,
                    EventFieldNameDictionary.VIEWS,
                    EventSQLiteHelper.MY_LIKE_STATUS,
            };

    private SQLiteDatabase database;
    private EventSQLiteHelper eventSQLHelper;

    public EventStatsDataSource(Context context, String dataBaseName, int databaseVersion) {
        eventSQLHelper = EventSQLiteHelper.getInstance(context, dataBaseName, databaseVersion);
    }

    public void openForWrite() throws SQLException {
        database = eventSQLHelper.getWritableDatabase();
    }

    public void createOrUpdateEventStats(@NonNull EventStats eventStats) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EventSQLiteHelper.EVENT_ID, eventStats.id);
        contentValues.put(EventFieldNameDictionary.LIKES, eventStats.likeCount);
        contentValues.put(EventFieldNameDictionary.VIEWS, eventStats.viewCount);
        contentValues.put(EventSQLiteHelper.MY_LIKE_STATUS, eventStats.myLikeStatus);
        database.insertWithOnConflict(EventSQLiteHelper.EVENT_STATS_TABLE_NAME, EventSQLiteHelper.EVENT_ID, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Nullable
    public EventStats getByEventId(int id) {
        EventStats eventStats = null;
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_STATS_TABLE_NAME,
                ALL_COLUMNS, EventSQLiteHelper.EVENT_ID + "=" + id, null, null, null, null);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            eventStats = cursorToEventItemStats(cursor);
        }
        cursor.close();
        return eventStats;
    }

    @NonNull
    private EventStats cursorToEventItemStats(@NonNull Cursor cursor) {
        EventStats eventStats = new EventStats();
        eventStats.id = cursor.getInt(cursor.getColumnIndex(EventSQLiteHelper.EVENT_ID));
        eventStats.viewCount = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.VIEWS));
        eventStats.likeCount = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.LIKES));
        eventStats.myLikeStatus = cursor.getInt(cursor.getColumnIndex(EventSQLiteHelper.MY_LIKE_STATUS));
        return eventStats;
    }

    public void removeEventByEventId(int id) {
    database.delete(EventSQLiteHelper.EVENT_STATS_TABLE_NAME, String.format("%s = %s", EventSQLiteHelper.EVENT_ID, id), null);
    }

    public void clear() {
        database.delete(EventSQLiteHelper.EVENT_STATS_TABLE_NAME, null, null);
    }
}
