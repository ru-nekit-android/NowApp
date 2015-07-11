package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.util.Pair;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.model.vo.EventToCalendarLink;

/**
 * Created by chuvac on 30.04.15.
 */
public class EventToCalendarLoader extends AsyncTaskLoader<Pair<Integer, EventToCalendarLink>> {

    public static final String KEY_EVENT_ITEM_ID = "event_item_id_key";
    public static final String KEY_METHOD = "method_key";

    public static final int CHECK = 1;
    public static final int ADD = 2;
    public static final int REMOVE = 3;

    private Bundle mArgs;

    public EventToCalendarLoader(@NonNull Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Nullable
    @Override
    public Pair<Integer, EventToCalendarLink> loadInBackground() {
        int method = mArgs.getInt(KEY_METHOD);
        return new Pair<>(method, NowApplication.getEventModel().performCalendarFunctionality(method, mArgs.getInt(KEY_EVENT_ITEM_ID)));
    }
}
