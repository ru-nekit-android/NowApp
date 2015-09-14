package ru.nekit.android.nowapp.model.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import org.json.JSONException;

import java.io.IOException;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.model.EventsModel;

/**
 * Created by chuvac on 13.03.15.
 */
public class EventLoader extends AsyncTaskLoader<Integer> {

    private Bundle mArgs;

    public EventLoader(@NonNull Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Integer loadInBackground() {
        try {
            return NowApplication.getInstance().getEventModel().performEventsLoad(mArgs.getString(EventsModel.LOADING_TYPE));
        } catch (@NonNull IOException | JSONException exp) {
            return EventsModel.INTERNAL_ERROR;
        }
    }

}
