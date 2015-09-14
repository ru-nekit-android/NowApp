package ru.nekit.android.nowapp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;

import com.madx.updatechecker.lib.UpdateChecker;
import com.madx.updatechecker.lib.UpdateCheckerListener;
//import com.squareup.leakcanary.LeakCanary;
//import com.squareup.leakcanary.RefWatcher;

import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.model.EventsModel;
import ru.nekit.android.nowapp.utils.ConnectionUtil;
import ru.nekit.android.nowapp.utils.ConnectivityReceiver;
import ru.nekit.android.nowapp.utils.VTAG;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE.OFFLINE;
import static ru.nekit.android.nowapp.NowApplication.APP_STATE.ONLINE;

/**
 * Created by chuvac on 17.03.15.
 */
public class NowApplication extends Application implements ConnectivityReceiver.OnNetworkAvailableListener, UpdateCheckerListener {

    public static final String CHANGE_APPLICATION_STATE_NOTIFICATION = "ru.nekit.android.change_application_state";
    public static final int VALID_DATA_PERIOD_HOURS = 24;
    public static final int VERSION = 18;

    private static final String LAST_UPDATE_TIME_KEY = "last_update_time_key";
    private static NowApplication instance;
    private APP_STATE mState;
    private SharedPreferences mSharedPreferences;
    private ConnectivityReceiver mConnectivityReceiver;
    private EventsModel mEventModel;
    //private RefWatcher mWatcher;

    /*public RefWatcher getWatcher() {
        return mWatcher;
    }*/

    public NowApplication() {
        super();
    }

    public void updateDataTimestamp() {
        mSharedPreferences.edit().putLong(LAST_UPDATE_TIME_KEY, System.currentTimeMillis()).apply();
    }

    @NonNull
    public OFFLINE_STATE getOfflineState() {
        long lastDataUpdateTimestamp = mSharedPreferences.getLong(LAST_UPDATE_TIME_KEY, -1);
        if (lastDataUpdateTimestamp == -1) {
            return OFFLINE_STATE.DATA_IS_EMPTY;
        }
        return Math.abs(System.currentTimeMillis() - lastDataUpdateTimestamp) < TimeUnit.HOURS.toMillis(VALID_DATA_PERIOD_HOURS) ? OFFLINE_STATE.DATA_IS_UP_TO_DATE : OFFLINE_STATE.DATA_IS_OUT_OF_DATE;
    }

    public APP_STATE getState() {
        return mState;
    }

    public void setState(APP_STATE state) {
        if (mState != state) {
            mState = state;
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(CHANGE_APPLICATION_STATE_NOTIFICATION));
        }
    }

    public void registerForAppChangeStateNotification(BroadcastReceiver changeApplicationStateReceiver) {
        LocalBroadcastManager.getInstance(this).registerReceiver(changeApplicationStateReceiver, new IntentFilter(NowApplication.CHANGE_APPLICATION_STATE_NOTIFICATION));
    }

    public void unregisterForAppChangeStateNotification(BroadcastReceiver changeApplicationStateReceiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(changeApplicationStateReceiver);
    }

    public void setConnectionReceiverActive(boolean value) {
        if (value) {
            if (ConnectionUtil.isInternetAvailable(this)) {
                setState(APP_STATE.ONLINE);
            } else {
                setState(APP_STATE.OFFLINE);
            }
            mConnectivityReceiver.bind(instance);
        } else {
            mConnectivityReceiver.unbind(instance);
        }
    }

    public void checkForUpdate() {
        new UpdateChecker(this, this).start();
    }

    @Override
    public void onCreate() {

        super.onCreate();

        instance = this;

        mEventModel = new EventsModel(this);

        //mWatcher = LeakCanary.install(this);

        mSharedPreferences = getSharedPreferences("nowapp", Context.MODE_PRIVATE);
        mConnectivityReceiver = new ConnectivityReceiver(this);
        mConnectivityReceiver.setOnNetworkAvailableListener(this);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        VTAG.call("metrics density: " + metrics.density);
    }

    @Override
    public void onNetworkAvailable() {
        setState(ONLINE);
    }

    @Override
    public void onNetworkUnavailable() {
        setState(OFFLINE);
    }

    @Override
    public void onNewVersion(String version) {

    }

    public enum APP_STATE {
        ONLINE,
        OFFLINE
    }

    public enum OFFLINE_STATE {
        DATA_IS_UP_TO_DATE,
        DATA_IS_OUT_OF_DATE,
        DATA_IS_EMPTY
    }

    public EventsModel getEventModel() {
        return mEventModel;
    }

    public static NowApplication getInstance() {
        return instance;
    }
}
