package ru.nekit.android.nowapp.mvvm;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by MacOS on 29.09.15.
 */
public class ApiCallResult {
@com.fasterxml.jackson.annotation.JsonInclude
    public String code;
    public String message;
    public Object data;
}

