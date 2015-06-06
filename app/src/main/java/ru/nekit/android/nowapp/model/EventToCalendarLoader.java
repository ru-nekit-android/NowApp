package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.util.Pair;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;

/**
 * Created by chuvac on 30.04.15.
 */
public class EventToCalendarLoader extends AsyncTaskLoader<Pair<Integer, EventToCalendarLink>> {

    public static final String EVENT_ITEM_ID_KEY = "event_item_id_key";
    public static final String METHOD_KEY = "method_key";
    public static final int CHECK = 1;
    public static final int ADD = 2;
    public static final int REMOVE = 3;

    private Bundle mArgs;

    public EventToCalendarLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Pair<Integer, EventToCalendarLink> loadInBackground() {
        return NowApplication.getEventModel().performCalendarFunctionality(mArgs.getInt(METHOD_KEY), mArgs.getInt(EVENT_ITEM_ID_KEY));
    }
}
