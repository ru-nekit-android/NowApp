package ru.nekit.android.nowapp.model;

/**
 * Created by chuvac on 10.06.15.
 */
public class EventItemStats {

    public int id;
    public int viewCount;
    public int likeCount;
    public int myLikeStatus;

    public int getLikeCount() {
        return likeCount + (myLikeStatus == EventItemsModel.LIKED_NOT_CONFIRMED ? 1 : 0);
    }

    public int getViewCount(boolean online, boolean confirmed) {
        return viewCount + (!online || !confirmed ? 1 : 0);
    }
}
