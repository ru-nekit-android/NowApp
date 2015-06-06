package ru.nekit.android.nowapp.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventToCalendarSQLiteHelper extends SQLiteOpenHelper implements BaseColumns {


    public static final String TABLE_NAME = "event_to_calendar_link";
    private static EventToCalendarSQLiteHelper sInstance;

    public static final String EVENT_ID = EventSQLiteHelper._ID;
    public static final String CALENDAR_EVENT_ID = "calendar_event" + EventSQLiteHelper._ID;

    private static final String DATABASE_CREATE = "CREATE TABLE "
            + TABLE_NAME + " ("
            + EVENT_ID + " INTEGER UNIQUE, "
            + CALENDAR_EVENT_ID + " INTEGER"
            + ");";


    private EventToCalendarSQLiteHelper(Context context, String dataBaseName, int databaseVersion) {
        super(context, dataBaseName, null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(database);
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        onUpgrade(database, oldVersion, newVersion);
    }

    public static EventToCalendarSQLiteHelper getInstance(Context context, String dataBaseName, int databaseVersion) {
        if (sInstance == null) {
            sInstance = new EventToCalendarSQLiteHelper(context, dataBaseName, databaseVersion);
        }
        return sInstance;
    }
}
