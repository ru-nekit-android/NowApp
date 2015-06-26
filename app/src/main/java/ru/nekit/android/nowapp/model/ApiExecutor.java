package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import org.json.JSONException;

import java.io.IOException;

import ru.nekit.android.nowapp.NowApplication;

/**
 * Created by chuvac on 13.03.15.
 */
public class ApiExecutor extends AsyncTaskLoader<EventApiCallResult> {

    public static final String KEY_EVENT_ITEM_ID = "event_item_id_key";
    public static final String KEY_METHOD = "method_key";

    public static final int METHOD_OBTAIN_STATS = 1;
    public static final int METHOD_LIKE = 2;
    public static final int METHOD_UPDATE_VIEW = 3;
    public static final int METHOD_OBTAIN_EVENT = 4;

    private Bundle mArgs;

    public ApiExecutor(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public EventApiCallResult loadInBackground() {
        int method = mArgs.getInt(KEY_METHOD);
        int eventId = mArgs.getInt(KEY_EVENT_ITEM_ID);
        EventApiCallResult result = null;
        EventsModel model = NowApplication.getEventModel();
        try {
            switch (method) {
                case METHOD_OBTAIN_STATS:

                    result = model.performObtainEventStats(eventId);

                    break;

                case METHOD_LIKE:

                    result = model.performUpdateEventLike(eventId);

                    break;

                case METHOD_UPDATE_VIEW:

                    result = model.performUpdateEventView(eventId);

                    break;

                case METHOD_OBTAIN_EVENT:

                    result = model.performObtainEvent(eventId);

                    break;
            }
        } catch (IOException | JSONException exp) {
            result = new EventApiCallResult(0, null);
        }
        return result;
    }
}