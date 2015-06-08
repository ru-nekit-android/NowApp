package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import ru.nekit.android.nowapp.NowApplication;

/**
 * Created by chuvac on 13.03.15.
 */
public class EventApiCaller extends AsyncTaskLoader<Integer> {

    public static final String KEY_EVENT_ITEM_ID = "event_item_id_key";
    public static final String KEY_METHOD = "method_key";

    public static final int METHOD_GET_STATS = 1;
    public static final int METHOD_LIKE = 2;
    public static final int METHOD_UPDATE_VIEW = 3;
    private Bundle mArgs;

    public EventApiCaller(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Integer loadInBackground() {
        int method = mArgs.getInt(KEY_METHOD);
        int eventId = mArgs.getInt(KEY_EVENT_ITEM_ID);
        EventItemsModel model = NowApplication.getEventModel();
        switch (method) {
            case METHOD_GET_STATS:
                return model.performGetStats(eventId);

            case METHOD_LIKE:
                return 0;

            case METHOD_UPDATE_VIEW:
                return model.performUpdateView(eventId);
        }
        return 0;
    }

}
