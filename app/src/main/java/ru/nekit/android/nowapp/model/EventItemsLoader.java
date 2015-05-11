package ru.nekit.android.nowapp.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

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
        return NowApplication.getEventModel().performLoad(getContext(), mArgs);
    }

}
