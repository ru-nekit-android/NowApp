package ru.nekit.android.nowapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.model.EventItemsModel;

/**
 * Created by chuvac on 17.03.15.
 */
public class NowApplication extends Application {


    public static final String CHANGE_APPLICATION_STATE = "ru.nekit.android.change_application_state";

    private static final String LAST_UPDATE_TIME_KEY = "last_update_time_key";
    private static NowApplication instance;

    public enum STATE {
        ONLINE,
        OFFLINE,
        DEFAULT
    }

    private static STATE mState;
    private static EventItemsModel mEventModel;
    private static SharedPreferences mSharedPreferences;

    public NowApplication() {
        super();
        mState = STATE.DEFAULT;
        instance = this;
    }

    @Override
    public void onCreate() {
        mEventModel = EventItemsModel.getInstance(this);
        mSharedPreferences = getSharedPreferences("nowapp", Context.MODE_PRIVATE);
    }

    public static void updateDataTimestamp() {
        mSharedPreferences.edit().putLong(LAST_UPDATE_TIME_KEY, System.currentTimeMillis()).apply();
    }

    public static EventItemsModel getEventModel() {
        return mEventModel;
    }

    public static boolean offlineAllow() {
        long lastDataUpdateTimestamp = mSharedPreferences.getLong(LAST_UPDATE_TIME_KEY, -1);
        if (lastDataUpdateTimestamp == -1) {
            return false;
        }
        return Math.abs(System.currentTimeMillis() - lastDataUpdateTimestamp) < TimeUnit.HOURS.toMillis(24);
    }

    public static STATE getState() {
        return mState;
    }

    public static void setState(STATE state) {
        if (mState != state) {
            mState = state;
            LocalBroadcastManager.getInstance(instance).sendBroadcast(new Intent(CHANGE_APPLICATION_STATE));
        }
    }

}
