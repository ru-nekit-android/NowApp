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
    private static EventSQLiteHelper sInstance;
    private String mDataBaseName;

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS "
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


    private EventSQLiteHelper(Context context, String dataBaseName) {
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

    public synchronized static EventSQLiteHelper getInstance(Context context, String dataBaseName) {
        if (sInstance == null) {
            sInstance = new EventSQLiteHelper(context, dataBaseName);
        }
        return sInstance;
    }


}
