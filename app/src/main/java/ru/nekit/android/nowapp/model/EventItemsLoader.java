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
public class EventItemsLoader extends AsyncTaskLoader<Integer> {

    private Bundle mArgs;

    public EventItemsLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Integer loadInBackground() {
        try {
            return NowApplication.getEventModel().performEventsLoad(mArgs.getString(EventItemsModel.LOADING_TYPE));
        } catch (IOException | JSONException exp) {
            return EventItemsModel.RESULT_BAD;
        }
    }

}
