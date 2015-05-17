package ru.nekit.android.nowapp.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.nekit.android.nowapp.model.EventFieldNameDictionary;

/**
 * Created by chuvac on 21.04.15.
 */
public class EventsFTSSQLiteHelper extends SQLiteOpenHelper {


    public static final String TABLE_NAME = "events_fts";
    public static final String FTS_ENGINE = "fts4";

    private static EventsFTSSQLiteHelper sInstance;

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE_FTS = "CREATE VIRTUAL TABLE " + TABLE_NAME +
            " USING " + FTS_ENGINE + "(" + EventFieldNameDictionary.ID + ", "
            + EventFieldNameDictionary.NAME + ", "
            + EventFieldNameDictionary.EVENT_DESCRIPTION + ", "
            + EventFieldNameDictionary.PLACE_NAME + ", "
            + EventFieldNameDictionary.ADDRESS + ");";

    private EventsFTSSQLiteHelper(Context context, String dataBaseName) {
        super(context, dataBaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_FTS);
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

    public synchronized static EventsFTSSQLiteHelper getInstance(Context context, String dataBaseName) {
        if (sInstance == null) {
            sInstance = new EventsFTSSQLiteHelper(context, dataBaseName);
        }
        return sInstance;
    }
}
