package ru.nekit.android.nowapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

/**
 * Created by chuvac on 28.03.15.
 */
public class ConnectionUtil {

    public static boolean isInternetAvailable(@NonNull Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if network is NOT available networkInfo will be null
        // otherwise check if we are connected
        return networkInfo != null && networkInfo.isConnected();

    }

    public static int getDataConnectionType(@NonNull Context ctx) {

        ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr != null && connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null) {
            if (connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()) {
                return ConnectivityManager.TYPE_MOBILE;
            } else if (connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                return ConnectivityManager.TYPE_WIFI;
            } else
                return -1;
        } else
            return -1;
    }

}
