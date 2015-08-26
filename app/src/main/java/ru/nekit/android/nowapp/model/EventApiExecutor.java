package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import ru.nekit.android.nowapp.NowApplication;

/**
 * Created by chuvac on 13.03.15.
 */
public class EventApiExecutor extends AsyncTaskLoader<EventApiCallResult> {

    public static final String KEY_EVENT_ITEM_ID = "event_item_id_key";
    public static final String KEY_METHOD = "method_key";

    public static final int METHOD_OBTAIN_STATS = 1;
    public static final int METHOD_LIKE = 2;
    public static final int METHOD_OBTAIN_EVENT = 3;

    private Bundle mArgs;

    public EventApiExecutor(@NonNull Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Nullable
    @Override
    public EventApiCallResult loadInBackground() {

        int method = mArgs.getInt(KEY_METHOD);
        int eventId = mArgs.getInt(KEY_EVENT_ITEM_ID);
        EventApiCallResult result = null;
        EventsModel model = NowApplication.getEventModel();

        if(model.deviceToken == null){
            model.performRegisterDevice();
        }

        switch (method) {
            case METHOD_OBTAIN_STATS:

                result = model.performObtainEventStats(eventId);

                break;

            case METHOD_LIKE:

                result = model.performEventLike(eventId);

                break;

            case METHOD_OBTAIN_EVENT:

                result = model.performObtainEvent(eventId);

                break;

            default:
        }

        return result;
    }
}