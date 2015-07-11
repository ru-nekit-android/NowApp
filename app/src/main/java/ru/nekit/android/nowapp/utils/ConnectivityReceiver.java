package ru.nekit.android.nowapp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

/**
 * Created by chuvac on 22.04.15.
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    @NonNull
    private final ConnectivityManager connectivityManager;
    private OnNetworkAvailableListener onNetworkAvailableListener;
    private boolean connection = false;
    public ConnectivityReceiver(@NonNull Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkConnectionOnDemand();
    }

    public void bind(@NonNull Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, filter);
        checkConnectionOnDemand();
    }

    public void unbind(@NonNull Context context) {
        context.unregisterReceiver(this);
    }

    private void checkConnectionOnDemand() {
        final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null || info.getState() != NetworkInfo.State.CONNECTED) {
            if (connection) {
                connection = false;
                if (onNetworkAvailableListener != null)
                    onNetworkAvailableListener.onNetworkUnavailable();
            }
        } else {
            if (!connection) {
                connection = true;
                if (onNetworkAvailableListener != null)
                    onNetworkAvailableListener.onNetworkAvailable();
            }
        }
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (connection && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                connection = false;
                if (onNetworkAvailableListener != null) {
                    onNetworkAvailableListener.onNetworkUnavailable();
                }
            } else if (!connection && !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                connection = true;
                if (onNetworkAvailableListener != null) {
                    onNetworkAvailableListener.onNetworkAvailable();
                }
            }
        }
    }

    public boolean hasConnection() {
        return connection;
    }

    public void setOnNetworkAvailableListener(OnNetworkAvailableListener listener) {
        this.onNetworkAvailableListener = listener;
    }

    public interface OnNetworkAvailableListener {
        void onNetworkAvailable();

        void onNetworkUnavailable();
    }

}