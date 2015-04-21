package ru.nekit.android.nowapp;

import android.app.Application;
import android.provider.Settings;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.model.EventViewStatistic;

/**
 * Created by chuvac on 17.03.15.
 */
public class NowApplication extends Application {

    private EventItemsModel mEventModel;
    private static String mDeviceId;

    public NowApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mEventModel = EventItemsModel.getInstance();

        ParseObject.registerSubclass(EventViewStatistic.class);
        //Parse.enableLocalDatastore(this);
        Parse.initialize(this, "b4xSfJNum1IqVebEnxsJZswuktwagNt1UhI2aZOR", "yZzxnfStQ5GtkQdQYApvYNyDIplajsIVzbIAyWj8");

        ParseUser.enableAutomaticUser();
        ParseACL acl = new ParseACL();

        acl.setPublicReadAccess(true);

        ParseACL.setDefaultACL(acl, true);

        mDeviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public EventItemsModel getEventModel() {
        return mEventModel;
    }

    public static final String getDeviceId() {
        return mDeviceId;
    }

}
