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

    private static final HashMap<String, Integer> CATEGORY_TYPE = new HashMap();
    private static final HashMap<String, Integer> CATEGORY_TYPE_BIG = new HashMap();

    private static final EventItemsModel instance;

    private ArrayList<EventItem> mEventItemsList;
    private int mEventItemCountOnServer;

    static {
        instance = new EventItemsModel();
    }

    private EventItemsModel() {
        mEventItemsList = new ArrayList<>();
        CATEGORY_TYPE.put("category_sport", R.drawable.cat_sport);
        CATEGORY_TYPE.put("category_entertainment", R.drawable.cat_drink);
        CATEGORY_TYPE.put("category_other", R.drawable.cat_crown);
        CATEGORY_TYPE.put("category_education", R.drawable.cat_book);
        CATEGORY_TYPE_BIG.put("category_sport", R.drawable.cat_sport_big);
        CATEGORY_TYPE_BIG.put("category_entertainment", R.drawable.cat_drink_big);
        CATEGORY_TYPE_BIG.put("category_other", R.drawable.cat_crown_big);
        CATEGORY_TYPE_BIG.put("category_education", R.drawable.cat_book_big);
    }

    public static int getCategoryDrawable(String category) {
        return CATEGORY_TYPE.get(category);

    }

    public static int getCategoryBigDrawable(String category) {
        return CATEGORY_TYPE_BIG.get(category);
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
                //int index = getIndexWithId(eventItem.id);
               // if (index > -1) {
               //     mEventItemsList.remove(index);
              //      mEventItemsList.add(index, eventItem);
              //  } else {
                    mEventItemsList.add(eventItem);
              //  }
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

    public int getIndexWithId(int id) {
        for (int i = 0; i < mEventItemsList.size(); i++) {
            if (mEventItemsList.get(i).id == id) {
                return i;
            }
        }
        return -1;
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
