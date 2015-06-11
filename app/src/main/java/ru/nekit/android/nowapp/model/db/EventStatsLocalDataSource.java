package ru.nekit.android.nowapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;
import ru.nekit.android.nowapp.model.EventItemStats;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventStatsLocalDataSource {

    private static final String[] ALL_COLUMNS =
            {
                    EventSQLiteHelper.EVENT_ID,
                    EventFieldNameDictionary.LIKE_COUNT,
                    EventFieldNameDictionary.VIEW_COUNT,
                    EventSQLiteHelper.MY_LIKE_STATUS,
            };

    private SQLiteDatabase database;
    private EventSQLiteHelper eventSQLHelper;

    public EventStatsLocalDataSource(Context context, String dataBaseName, int databaseVersion) {
        eventSQLHelper = EventSQLiteHelper.getInstance(context, dataBaseName, databaseVersion);
    }

    public void openForWrite() throws SQLException {
        database = eventSQLHelper.getWritableDatabase();
    }

    public void createOrUpdateEventStats(EventItemStats eventItemStats) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EventSQLiteHelper.EVENT_ID, eventItemStats.id);
        contentValues.put(EventFieldNameDictionary.LIKE_COUNT, eventItemStats.likeCount);
        contentValues.put(EventFieldNameDictionary.VIEW_COUNT, eventItemStats.viewCount);
        contentValues.put(EventSQLiteHelper.MY_LIKE_STATUS, eventItemStats.myLikeStatus);
        database.insertWithOnConflict(EventSQLiteHelper.EVENT_STATS_TABLE_NAME, EventSQLiteHelper.EVENT_ID, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public EventItemStats getByEventId(int id) {
        EventItemStats eventItemStats = null;
        Cursor cursor = database.query(EventSQLiteHelper.EVENT_STATS_TABLE_NAME,
                ALL_COLUMNS, EventSQLiteHelper.EVENT_ID + "=" + id, null, null, null, null);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            eventItemStats = cursorToEventItemStats(cursor);
        }
        cursor.close();
        return eventItemStats;
    }

    private EventItemStats cursorToEventItemStats(Cursor cursor) {
        EventItemStats eventItemStats = new EventItemStats();
        eventItemStats.id = cursor.getInt(cursor.getColumnIndex(EventSQLiteHelper.EVENT_ID));
        eventItemStats.viewCount = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.VIEW_COUNT));
        eventItemStats.likeCount = cursor.getInt(cursor.getColumnIndex(EventFieldNameDictionary.LIKE_COUNT));
        eventItemStats.myLikeStatus = cursor.getInt(cursor.getColumnIndex(EventSQLiteHelper.MY_LIKE_STATUS));
        return eventItemStats;
    }

    public void clear() {
        database.delete(EventSQLiteHelper.EVENT_STATS_TABLE_NAME, null, null);
    }
}
