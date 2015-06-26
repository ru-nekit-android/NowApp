package ru.nekit.android.nowapp.model;

import ru.nekit.android.nowapp.model.vo.Event;

/**
 * Created by chuvac on 09.06.15.
 */
public class EventApiCallResult {

    public Event event;
    public int result;

    public EventApiCallResult(int result, Event event) {
        this.result = result;
        this.event = event;
    }

}
