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
    public boolean likedByMe;
    public int myLikeStatus;

    private int getLikeCount() {
        return likeCount + (myLikeStatus == EventsModel.LIKE_NOT_CONFIRMED && likedByMe ? 1 : 0);
    }

    @NonNull
    public String getLikes() {
        int likes = getLikeCount();
        return likes > MAX_LIKES ? Integer.toString(MAX_LIKES) + "+" : Integer.toString(likes);
    }


    @NonNull
    public String getViews() {
        return viewCount > MAX_VIEWS ? Integer.toString(MAX_VIEWS) + "+" : Integer.toString(viewCount);
    }

}
