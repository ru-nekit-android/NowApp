package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import org.json.JSONException;

import java.io.IOException;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.model.vo.ApiCallResult;

/**
 * Created by chuvac on 13.03.15.
 */
public class EventApiExecutor extends AsyncTaskLoader<ApiCallResult> {

    public static final String KEY_EVENT_ITEM_ID = "event_item_id_key";
    public static final String KEY_METHOD = "method_key";

    public static final int METHOD_GET_STATS = 1;
    public static final int METHOD_LIKE = 2;
    public static final int METHOD_UPDATE_VIEW = 3;

    private Bundle mArgs;

    public EventApiExecutor(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public ApiCallResult loadInBackground() {
        int method = mArgs.getInt(KEY_METHOD);
        int eventId = mArgs.getInt(KEY_EVENT_ITEM_ID);
        int result = 0;
        EventItemsModel model = NowApplication.getEventModel();
        try {
            switch (method) {
                case METHOD_GET_STATS:

                    result = model.performGetStats(eventId);

                    break;

                case METHOD_LIKE:

                    result = model.performEventUpdateLike(eventId);

                    break;

                case METHOD_UPDATE_VIEW:

                    result = model.performEventUpdateView(eventId);

                    break;
            }
        } catch (IOException | JSONException exp) {
            result = EventItemsModel.RESULT_BAD;
        }
        return new ApiCallResult(method, result);
    }
}