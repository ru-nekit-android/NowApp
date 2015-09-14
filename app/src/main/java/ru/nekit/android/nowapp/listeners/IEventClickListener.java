package ru.nekit.android.nowapp.listeners;

import ru.nekit.android.nowapp.model.vo.Event;

/**
 * Created by chuvac on 13.03.15.
 */

public interface IEventClickListener {
    void onEventClick(Event event, boolean openNew);
}
