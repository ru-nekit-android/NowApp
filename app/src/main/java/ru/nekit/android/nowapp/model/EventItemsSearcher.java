package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;

import ru.nekit.android.nowapp.NowApplication;

/**
 * Created by chuvac on 13.03.15.
 */
public class EventItemsSearcher extends AsyncTaskLoader<ArrayList<EventItem>> {

    public static final String EVENT_ITEMS_SEARCH_KEY = "event_items_search_key";

    private Bundle mArgs;

    public EventItemsSearcher(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public ArrayList<EventItem> loadInBackground() {
        return NowApplication.getEventModel().performSearch(mArgs.getString(EVENT_ITEMS_SEARCH_KEY));
    }

}
