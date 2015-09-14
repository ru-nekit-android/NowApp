package ru.nekit.android.nowapp.model.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.model.EventsModel;
import ru.nekit.android.nowapp.model.vo.Event;

/**
 * Created by chuvac on 13.03.15.
 */
public class EventItemsLoader extends AsyncTaskLoader<ArrayList<Event>> {

    public static final String KEY_EVENT_ITEMS_SEARCH = "event_items_search_key";

    @NonNull
    private Bundle mArgs;

    public EventItemsLoader(@NonNull Context context, @NonNull Bundle args) {
        super(context);
        mArgs = args;
    }

    @NonNull
    @Override
    public ArrayList<Event> loadInBackground() {
        return NowApplication.getInstance().getEventModel().performSearch(mArgs.getString(KEY_EVENT_ITEMS_SEARCH));
    }

    @Override
    public void deliverResult(ArrayList<Event> data) {
        if (isReset()) {
            return;
        }
        super.deliverResult(data);
    }


    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

}
