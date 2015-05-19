package ru.nekit.android.nowapp.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventSQLiteHelper extends SQLiteOpenHelper {


    public static final String TABLE_NAME = "events";
    public static final String FTS_TABLE_NAME = "events_fts";
    public static final String FTS_ENGINE = "fts4";

    private static EventSQLiteHelper sInstance;

    private static final String DATABASE_CREATE = "CREATE TABLE "
            + TABLE_NAME + " ("
            + EventFieldNameDictionary.ID + " INTEGER PRIMARY KEY, "
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

    private static final String DATABASE_CREATE_FTS = "CREATE VIRTUAL TABLE " + FTS_TABLE_NAME +
            " USING " + FTS_ENGINE + "(" + EventFieldNameDictionary.ID + " integer unique, "
            + EventFieldNameDictionary.NAME + ", "
            + EventFieldNameDictionary.EVENT_DESCRIPTION + ", "
            + EventFieldNameDictionary.PLACE_NAME + ", "
            + EventFieldNameDictionary.ADDRESS + ");";

    private EventSQLiteHelper(Context context, String dataBaseName, int databaseVersion) {
        super(context, dataBaseName, null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(DATABASE_CREATE_FTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + FTS_TABLE_NAME);
        onCreate(database);
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        onUpgrade(database, oldVersion, newVersion);
    }

    public synchronized static EventSQLiteHelper getInstance(Context context, String dataBaseName, int databaseVersion) {
        if (sInstance == null) {
            sInstance = new EventSQLiteHelper(context, dataBaseName, databaseVersion);
        }
        return sInstance;
    }


}
