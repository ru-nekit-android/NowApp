package ru.nekit.android.nowapp;

import android.app.Application;

import ru.nekit.android.nowapp.model.EventItemsModel;

/**
 * Created by chuvac on 17.03.15.
 */
public class NowApplication extends Application {

    public static enum STATE {
        ONLINE,
        OFFLINE
    }

    private static STATE mState;
    private EventItemsModel mEventModel;
    private static String mDeviceId;


    public NowApplication() {
        super();
    }

    @Override
    public void onCreate() {

        mEventModel = EventItemsModel.getInstance(this);

    }

    public EventItemsModel getEventModel() {
        return mEventModel;
    }


    public static STATE getState() {
        return mState;
    }

    public static void setState(STATE state) {
        mState = state;
    }

}
