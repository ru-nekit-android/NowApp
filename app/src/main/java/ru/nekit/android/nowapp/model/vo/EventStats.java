package ru.nekit.android.nowapp.model.vo;

import android.support.annotation.NonNull;

import ru.nekit.android.nowapp.model.EventsModel;

/**
 * Created by chuvac on 10.06.15.
 */
public class EventStats {


    public static final int MAX_VIEWS = 10000;
    public static final int MAX_LIKES = 1000;

    public int id;
    public int viewCount;
    public int likeCount;
    public int myLikeStatus;

    private int getLikeCount() {
        return likeCount + (myLikeStatus == EventsModel.LIKE_NOT_CONFIRMED ? 1 : 0);
    }

    @NonNull
    public String getLikes() {
        int likes = getLikeCount();
        return likes > MAX_LIKES ? Integer.toString(MAX_LIKES) + "+" : Integer.toString(likes);
    }

    private int getViewCount(boolean online, boolean confirmed) {
        return viewCount + (!online || !confirmed ? 1 : 0);
    }

    @NonNull
    public String getViews(boolean online, boolean confirmed) {
        int views = getViewCount(online, confirmed);
        return views > MAX_VIEWS ? Integer.toString(MAX_VIEWS) + "+" : Integer.toString(views);
    }

}
