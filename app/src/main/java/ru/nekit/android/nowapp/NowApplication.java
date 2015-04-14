package ru.nekit.android.nowapp;

import android.app.Application;
import android.util.DisplayMetrics;
import android.util.Log;

import ru.nekit.android.nowapp.model.EventItemsModel;

/**
 * Created by chuvac on 17.03.15.
 */
public class NowApplication extends Application {

    private EventItemsModel mEventModel;

    public NowApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mEventModel = EventItemsModel.getInstance();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Log.v("ru.nekit.vtag", "!"+metrics.density);
    }

    public EventItemsModel getEventModel() {
        return mEventModel;
    }

}
