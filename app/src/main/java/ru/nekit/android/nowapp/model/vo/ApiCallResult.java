package ru.nekit.android.nowapp.model.vo;

/**
 * Created by chuvac on 09.06.15.
 */
public class ApiCallResult {

    private int method;
    private int result;

    public ApiCallResult(int method, int result) {
        this.method = method;
        this.result = result;
    }

    public long getMethod() {
        return method;
    }

    public long getResult() {
        return result;
    }


}
