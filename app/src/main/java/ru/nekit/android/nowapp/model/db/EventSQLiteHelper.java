package ru.nekit.android.nowapp.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventSQLiteHelper extends SQLiteOpenHelper implements BaseColumns {

    public static final String EVENT_TABLE_NAME = "events";
    public static final String FTS_TABLE_NAME = "events_fts";
    public static final String FTS_ENGINE = "fts4";
    public static final String EVENT_TO_CALENDAR_LINK_TABLE_NAME = "event_to_calendar_link";
    public static final String EVENT_STATS_TABLE_NAME = "event_stats";
    public static final String EVENT_ADVERT_TABLE_NAME = "event_advert";

    public static final String EVENT_ID = EventSQLiteHelper._ID;
    public static final String EVENT_ADVERT_ID = EventSQLiteHelper._ID;
    public static final String CALENDAR_EVENT_ID = "calendar_event" + EventSQLiteHelper._ID;
    public static final String MY_LIKE_STATUS = "my_like_status";

    static final String FTS_EVENT_START_TIME_ALIAS = "event_start_time_alias";
    static final String FTS_EVENT_CATEGORY_KEYWORD = "event_category_keyword";
    static final String FTS_EVENT_START_TIME = "event_start_time";

    private static final String EVENT_TO_CALENDAR_LINK_TABLE_CREATE = "CREATE TABLE "
            + EVENT_TO_CALENDAR_LINK_TABLE_NAME + " ("
            + EVENT_ID + " INTEGER NOT NULL, "
            + CALENDAR_EVENT_ID + " INTEGER"
            + ");";

    private static final String EVENT_STATS_TABLE_CREATE = "CREATE TABLE "
            + EVENT_STATS_TABLE_NAME + " ("
            + EVENT_ID + " INTEGER UNIQUE NOT NULL, "
            + EventFieldNameDictionary.LIKES + " INTEGER, "
            + EventFieldNameDictionary.VIEWS + " INTEGER, "
            + MY_LIKE_STATUS + " INTEGER"
            + ");";

    private static final String EVENT_TABLE_CREATE = "CREATE TABLE "
            + EVENT_TABLE_NAME + " ("
            + EVENT_ID + " INTEGER PRIMARY KEY, "
            + EventFieldNameDictionary.ADDRESS + " TEXT NOT NULL, "
            + EventFieldNameDictionary.ALL_NIGHT_PARTY + " INTEGER DEFAULT 0, "
            + EventFieldNameDictionary.DATE + " INTEGER NOT NULL, "
            + EventFieldNameDictionary.EMAIL + " TEXT, "
            + EventFieldNameDictionary.END_AT + " INTEGER NOT NULL, "
            + EventFieldNameDictionary.START_AT + " INTEGER NOT NULL, "
            + EventFieldNameDictionary.ENTRANCE + " TEXT, "
            + EventFieldNameDictionary.EVENT_CATEGORY + " TEXT, "
            + EventFieldNameDictionary.NAME + " TEXT, "
            + EventFieldNameDictionary.EVENT_DESCRIPTION + " TEXT, "
            + EventFieldNameDictionary.LOGO_ORIGINAL + " TEXT, "
            + EventFieldNameDictionary.LOGO_THUMB + " TEXT, "
            + EventFieldNameDictionary.EVENT_GEO_POSITION_LATITUDE + " REAL, "
            + EventFieldNameDictionary.EVENT_GEO_POSITION_LONGITUDE + " REAL, "
            + EventFieldNameDictionary.PHONE + " TEXT, "
            + EventFieldNameDictionary.SITE + " TEXT, "
            + EventFieldNameDictionary.PLACE_ID + " INTEGER, "
            + EventFieldNameDictionary.PLACE_NAME + " TEXT, "
            + EventFieldNameDictionary.POSTER_BLUR + " TEXT, "
            + EventFieldNameDictionary.POSTER_ORIGINAL + " TEXT, "
            + EventFieldNameDictionary.POSTER_THUMB + " TEXT"
            + ");";
    private static final String EVENT_FTS_TABLE_CREATE = "CREATE VIRTUAL TABLE " + FTS_TABLE_NAME +
            " USING " + FTS_ENGINE + "(" + EVENT_ID + " INTEGER UNIQUE, "
            + EventFieldNameDictionary.NAME + ", "
            + EventFieldNameDictionary.EVENT_DESCRIPTION + " TEXT, "
            + EventFieldNameDictionary.PLACE_NAME + " TEXT, "
            + EventFieldNameDictionary.ADDRESS + " TEXT, "
            + FTS_EVENT_START_TIME_ALIAS + " TEXT, "
            + FTS_EVENT_CATEGORY_KEYWORD + " TEXT, "
            + FTS_EVENT_START_TIME + " INTEGER"
            + ");";

    private static final String EVENT_ADVERT_TABLE_CREATE = "CREATE TABLE "
            + EVENT_ADVERT_TABLE_NAME + " ("
            + EVENT_ADVERT_ID + " INTEGER UNIQUE, "
            + EventFieldNameDictionary.ADVERT.EVENT_ID + " INTEGER UNIQUE NOT NULL, "
            + EventFieldNameDictionary.ADVERT.START_AT + " INTEGER NOT NULL, "
            + EventFieldNameDictionary.ADVERT.LOGO_THUMB + " TEXT, "
            + EventFieldNameDictionary.ADVERT.LINK + " TEXT, "
            + EventFieldNameDictionary.ADVERT.NAME + " TEXT, "
            + EventFieldNameDictionary.ADVERT.PLACE_NAME + " TEXT, "
            + EventFieldNameDictionary.ADVERT.AD_TYPE + " TEXT, "
            + EventFieldNameDictionary.ADVERT.SHOW_CHANCE_LOW + " INTEGER, "
            + EventFieldNameDictionary.ADVERT.SHOW_CHANCE_HIGH + " INTEGER"
            + ");";

    private static EventSQLiteHelper sInstance;

    private EventSQLiteHelper(Context context, String dataBaseName, int databaseVersion) {
        super(context, dataBaseName, null, databaseVersion);
    }

    public synchronized static EventSQLiteHelper getInstance(Context context, String dataBaseName, int databaseVersion) {
        if (sInstance == null) {
            sInstance = new EventSQLiteHelper(context, dataBaseName, databaseVersion);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(EVENT_TABLE_CREATE);
        database.execSQL(EVENT_FTS_TABLE_CREATE);
        database.execSQL(EVENT_TO_CALENDAR_LINK_TABLE_CREATE);
        database.execSQL(EVENT_STATS_TABLE_CREATE);
        database.execSQL(EVENT_ADVERT_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + EVENT_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + FTS_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + EVENT_TO_CALENDAR_LINK_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + EVENT_STATS_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + EVENT_ADVERT_TABLE_NAME);
        onCreate(database);
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        onUpgrade(database, oldVersion, newVersion);
    }
}
