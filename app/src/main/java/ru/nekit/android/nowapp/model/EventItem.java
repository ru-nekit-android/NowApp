package ru.nekit.android.nowapp.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chuvac on 12.03.15.
 */
public class EventItem implements Parcelable {

    public static final Parcelable.Creator<EventItem> CREATOR = new Parcelable.Creator<EventItem>() {
        public EventItem createFromParcel(Parcel source) {
            return new EventItem(source);
        }

        public EventItem[] newArray(int size) {
            return new EventItem[size];
        }
    };
    public int placeId;
    public int id;
    public long date;
    public String eventDescription;
    public String category;
    public String entrance;
    public String placeName;
    public String address;
    public String phone;
    public String site;
    public String email;
    public String name;
    public long startAt;
    public long endAt;
    public String posterThumb;
    public String posterBlur;
    public String posterOriginal;
    public String logoThumb;
    public String logoOriginal;
    public int allNightParty;
    public double lat;
    public double lng;

    //Internal part
    public int likeCount;
    public int viewCount;

    public EventItem() {
    }

    private EventItem(Parcel in) {
        this.placeId = in.readInt();
        this.id = in.readInt();
        this.date = in.readLong();
        this.eventDescription = in.readString();
        this.category = in.readString();
        this.entrance = in.readString();
        this.placeName = in.readString();
        this.address = in.readString();
        this.phone = in.readString();
        this.site = in.readString();
        this.email = in.readString();
        this.name = in.readString();
        this.startAt = in.readLong();
        this.endAt = in.readLong();
        this.posterThumb = in.readString();
        this.posterBlur = in.readString();
        this.posterOriginal = in.readString();
        this.logoThumb = in.readString();
        this.logoOriginal = in.readString();
        this.allNightParty = in.readInt();
        this.lat = in.readDouble();
        this.lng = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.placeId);
        dest.writeInt(this.id);
        dest.writeLong(this.date);
        dest.writeString(this.eventDescription);
        dest.writeString(this.category);
        dest.writeString(this.entrance);
        dest.writeString(this.placeName);
        dest.writeString(this.address);
        dest.writeString(this.phone);
        dest.writeString(this.site);
        dest.writeString(this.email);
        dest.writeString(this.name);
        dest.writeLong(this.startAt);
        dest.writeLong(this.endAt);
        dest.writeString(this.posterThumb);
        dest.writeString(this.posterBlur);
        dest.writeString(this.posterOriginal);
        dest.writeString(this.logoThumb);
        dest.writeString(this.logoOriginal);
        dest.writeInt(this.allNightParty);
        dest.writeDouble(this.lat);
        dest.writeDouble(this.lng);
    }
}
