package ru.nekit.android.nowapp.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventToCalendarSQLiteHelper extends SQLiteOpenHelper implements BaseColumns {


    public static final String TABLE_NAME = "event_to_calendar";
    private static EventToCalendarSQLiteHelper sInstance;
    private String mDataBaseName;

    private static final int DATABASE_VERSION = 1;
    public static final String EVENT_ID = "event_" + EventFieldNameDictionary.ID;
    public static final String CALENDAR_EVENT_ID = "calendar_event_" + EventFieldNameDictionary.ID;

    private static final String DATABASE_CREATE = "CREATE TABLE "
            + TABLE_NAME + " ("
            + EVENT_ID + " INTEGER UNIQUE, "
            + CALENDAR_EVENT_ID + " INTEGER"
            + ");";


    private EventToCalendarSQLiteHelper(Context context, String dataBaseName) {
        super(context, dataBaseName, null, DATABASE_VERSION);
        mDataBaseName = dataBaseName;
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

    public static EventToCalendarSQLiteHelper getInstance(Context context, String dataBaseName) {
        if (sInstance == null) {
            sInstance = new EventToCalendarSQLiteHelper(context, dataBaseName);
        }
        return sInstance;
    }
}
