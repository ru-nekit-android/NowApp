package ru.nekit.android.nowapp.mvvm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MacOS on 29.09.15.
 */
public class EventListApiCallResult {

    public String code;
    public String httpCode;
    public String message;
    public List<EventApiCallResult> events = new ArrayList<>();

}


