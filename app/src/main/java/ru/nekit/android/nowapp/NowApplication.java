package ru.nekit.android.nowapp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;

import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.utils.ConnectionUtil;
import ru.nekit.android.nowapp.utils.ConnectivityReceiver;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.OFFLINE;
import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

/**
 * Created by chuvac on 17.03.15.
 */
public class NowApplication extends Application implements ConnectivityReceiver.OnNetworkAvailableListener {


    public static final String CHANGE_APPLICATION_STATE_NOTIFICATION = "ru.nekit.android.change_application_state";
    public static final int VALID_DATA_PERIOD_HOURS = 24;

    private static final String LAST_UPDATE_TIME_KEY = "last_update_time_key";
    private static NowApplication instance;

    public enum APP_STATE {
        ONLINE,
        OFFLINE
    }

    public enum OFFLINE_STATE {
        DATA_IS_UP_TO_DATE,
        DATA_IS_OUT_OF_DATE,
        DATA_IS_EMPTY
    }

    private static APP_STATE mState;
    private static EventItemsModel mEventModel;
    private static SharedPreferences mSharedPreferences;
    private static ConnectivityReceiver mConnectivityReceiver;

    public NowApplication() {
        super();
        instance = this;
    }

    @Override
    public void onCreate() {

        mEventModel = EventItemsModel.getInstance(this);
        mSharedPreferences = getSharedPreferences("nowapp", Context.MODE_PRIVATE);
        mConnectivityReceiver = new ConnectivityReceiver(this);
        mConnectivityReceiver.setOnNetworkAvailableListener(this);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        VTAG.call("metrics density: " + metrics.density);
    }

    public static void updateDataTimestamp() {
        mSharedPreferences.edit().putLong(LAST_UPDATE_TIME_KEY, System.currentTimeMillis()).apply();
    }

    public static EventItemsModel getEventModel() {
        return mEventModel;
    }

    public static OFFLINE_STATE getOfflineState() {
        long lastDataUpdateTimestamp = mSharedPreferences.getLong(LAST_UPDATE_TIME_KEY, -1);
        if (lastDataUpdateTimestamp == -1) {
            return OFFLINE_STATE.DATA_IS_EMPTY;
        }
        return Math.abs(System.currentTimeMillis() - lastDataUpdateTimestamp) < TimeUnit.HOURS.toMillis(VALID_DATA_PERIOD_HOURS) ? OFFLINE_STATE.DATA_IS_UP_TO_DATE : OFFLINE_STATE.DATA_IS_OUT_OF_DATE;
    }

    public static APP_STATE getState() {
        return mState;
    }

    public static void setState(APP_STATE state) {
        if (mState != state) {
            mState = state;
            LocalBroadcastManager.getInstance(instance).sendBroadcast(new Intent(CHANGE_APPLICATION_STATE_NOTIFICATION));
        }
    }

    public static void registerForAppChangeStateNotification(BroadcastReceiver changeApplicationStateReceiver) {
        LocalBroadcastManager.getInstance(instance).registerReceiver(changeApplicationStateReceiver, new IntentFilter(NowApplication.CHANGE_APPLICATION_STATE_NOTIFICATION));
    }

    public static void unregisterForAppChangeStateNotification(BroadcastReceiver changeApplicationStateReceiver) {
        LocalBroadcastManager.getInstance(instance).unregisterReceiver(changeApplicationStateReceiver);
    }

    @Override
    public void onNetworkAvailable() {
        setState(ONLINE);
    }

    @Override
    public void onNetworkUnavailable() {
        setState(OFFLINE);
    }

    public static void setConnectionReceiverActive(boolean value) {
        if (value) {
            if (ConnectionUtil.isInternetAvailable(instance)) {
                setState(APP_STATE.ONLINE);
            } else {
                setState(APP_STATE.OFFLINE);
            }
            mConnectivityReceiver.bind(instance);
        } else {
            mConnectivityReceiver.unbind(instance);
        }
    }
}
