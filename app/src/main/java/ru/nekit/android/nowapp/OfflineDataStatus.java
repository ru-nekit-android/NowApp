package ru.nekit.android.nowapp;

/**
 * Created by MacOS on 29.09.15.
 */

public enum OfflineDataStatus {
    UNKNOWN("unknown"),
    IS_UP_TO_DATE("is_up_to_date"),
    IS_OUT_OF_DATE("is_out_of_date");

    private final String status;

    OfflineDataStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}

