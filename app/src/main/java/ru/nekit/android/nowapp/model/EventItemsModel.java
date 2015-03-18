package ru.nekit.android.nowapp.model;

import java.util.ArrayList;
import java.util.HashMap;

import ru.nekit.android.nowapp.R;

/**
 * Created by chuvac on 15.03.15.
 */
public class EventItemsModel {

    public static final String TYPE = "type";

    public static final String REQUEST_NEW_EVENT_ITEMS = "request_new_event_items";
    public static final String REFRESH_EVENT_ITEMS = "refresh_event_items";

    public static final HashMap<String, Integer> CATALOG_TYPE = new HashMap();

    private static final EventItemsModel instance;

    private ArrayList<EventItem> mEventItemsList;
    private int mEventItemCountOnServer;

    static {
        instance = new EventItemsModel();
    }

    private EventItemsModel() {
        mEventItemsList = new ArrayList<>();
        CATALOG_TYPE.put("category_sport", R.drawable.cat_sport);
        CATALOG_TYPE.put("category_entertainment", R.drawable.cat_drink);
        CATALOG_TYPE.put("category_other", R.drawable.cat_crown);
        CATALOG_TYPE.put("category_education", R.drawable.cat_book);
    }

    public static EventItemsModel getInstance() {
        return instance;
    }

    public void addEvents(ArrayList<EventItem> eventItems) {
        if (isEventItemsListEmpty()) {
            mEventItemsList.addAll(eventItems);
        } else {
            for (int i = 0; i < eventItems.size(); i++) {
                EventItem eventItem = eventItems.get(i);
                if (hasEventWithId(eventItem.id)) {
                    mEventItemsList.remove(i);
                    mEventItemsList.add(i, eventItem);
                } else {
                    mEventItemsList.add(eventItem);
                }
            }
        }
    }

    public void setEvents(ArrayList<EventItem> eventList) {
        mEventItemsList.clear();
        mEventItemsList.addAll(eventList);
    }

    public int getLastEventId() {
        return mEventItemsList.size() > 0 ? mEventItemsList.get(mEventItemsList.size() - 1).id : 0;
    }

    public ArrayList<EventItem> getEventItemsList() {
        return mEventItemsList;
    }

    public Boolean hasEventWithId(int id) {
        for (EventItem item : mEventItemsList) {
            if (item.id == id) {
                return true;
            }
        }
        return false;
    }

    public void setEventItemCountOnServer(int count) {
        mEventItemCountOnServer = count;
    }

    public int getEventItemCountOnServer() {
        return mEventItemCountOnServer;
    }

    public boolean isEventItemsListEmpty() {
        return mEventItemsList.size() == 0;
    }
}
