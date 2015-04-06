package ru.nekit.android.nowapp.modelView;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemSelectListener;

import static ru.nekit.android.nowapp.model.EventItemsModel.getCategoryDrawable;
import static ru.nekit.android.nowapp.model.EventItemsModel.getCurrentDateTimestamp;
import static ru.nekit.android.nowapp.model.EventItemsModel.getCurrentTimeTimestamp;

/**
 * Created by chuvac on 12.03.15.
 */
public class EventCollectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    final int NORMAL = 0, LOADING = 1;

    private int mItemHeight, mColumns, mMargin;
    private final LayoutInflater mInflater;
    private final Context mContext;

    private ArrayList<WrapperEventItem> mEventItems = new ArrayList<>();
    private IEventItemSelectListener mItemClickListener;

    public void setOnItemClickListener(IEventItemSelectListener listener) {
        mItemClickListener = listener;
    }

    public EventCollectionAdapter(Context context, ArrayList<EventItem> items, int columns) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mColumns = columns;
        int screenWidth = getScreenWidth(context);
        mItemHeight = (int) ((screenWidth / columns) * (mColumns > 1 ? .86 : .6));
        mMargin = context.getResources().getDimensionPixelSize(R.dimen.event_collection_space);
        setItems(items);
    }

    public void setItems(ArrayList<EventItem> items) {
        mEventItems.clear();
        if (items != null) {
            for (EventItem eventItem : items) {
                long dateDelta = eventItem.date - getCurrentDateTimestamp(mContext, true);
                if (!(dateDelta == 0 && eventItem.endAt < EventItemsModel.getCurrentTimeTimestamp(mContext, true)))
                {
                    mEventItems.add(new WrapperEventItem(eventItem));
                }
            }
        }
        notifyDataSetChanged();
    }

    public void clearItems() {
        mEventItems.clear();
        notifyDataSetChanged();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            return size.x;
        }
        return display.getWidth();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            return size.y;
        }
        return display.getHeight();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case LOADING:
                view = mInflater.inflate(R.layout.item_event_loading, parent, false);
                return new LoadingHolder(view);
            case NORMAL:
            default:
                view = mInflater.inflate(R.layout.item_event, parent, false);
                return new EventCollectionItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
        if (getItemViewType(position) == NORMAL) {
            layoutParams.height = mItemHeight;
            if (mColumns > 1) {
                if (position % mColumns == 0) {
                    layoutParams.setMargins(mMargin, mMargin, mMargin / 2, 0);
                } else {
                    layoutParams.setMargins(mMargin / 2, mMargin, mMargin, 0);
                }
            } else {
                layoutParams.setMargins(mMargin / 2, mMargin, mMargin / 2, 0);
            }
            EventCollectionItemViewHolder eventCollectionItemViewHolder = (EventCollectionItemViewHolder) viewHolder;
            final EventItem eventItem = mEventItems.get(position).eventItem;

            eventCollectionItemViewHolder.getPlaceView().setText(eventItem.placeName);
            String eventName = eventItem.name.toUpperCase();
            String[] eventNameArray = eventName.split(" ");
            eventCollectionItemViewHolder.getNameView().setLines(eventNameArray.length);
            eventCollectionItemViewHolder.getNameView().setText(eventName.replace(" ", "\n"));

            eventCollectionItemViewHolder.getPosterThumbView().setColorFilter(mContext.getResources().getColor(R.color.poster_overlay), android.graphics.PorterDuff.Mode.MULTIPLY);

            Glide.with(mContext).load(eventItem.posterBlur).centerCrop().dontAnimate().into(eventCollectionItemViewHolder.getPosterThumbView());

            long currentTimeTimestamp = getCurrentTimeTimestamp(mContext, true);
            long startAfterSeconds = eventItem.startAt - currentTimeTimestamp;

            long dateDelta = eventItem.date - getCurrentDateTimestamp(mContext, true);
            String startAfterString;

            if (dateDelta == 0) {
                if (startAfterSeconds <= 0) {
                    if (eventItem.endAt > currentTimeTimestamp) {
                        startAfterString = mContext.getResources().getString(R.string.going_right_now);
                    } else {
                        startAfterString = mContext.getResources().getString(R.string.already_ended);
                    }
                } else {
                    long startAfterMinutesFull = startAfterSeconds / 60;
                    long startAfterHours = startAfterMinutesFull / 60;
                    long startAfterMinutes = startAfterMinutesFull % 60;
                    startAfterString = mContext.getResources().getString(R.string.going_in);
                    if (startAfterHours > 0) {
                        startAfterString += String.format(" %d ч", startAfterHours);
                    }
                    if (startAfterMinutes > 0) {
                        startAfterString += String.format(" %d мин", startAfterMinutes);
                    }
                }
            } else {
                if (dateDelta == TimeUnit.DAYS.toSeconds(1)) {
                    startAfterString = mContext.getResources().getString(R.string.going_tomorrow);
                } else if (dateDelta == TimeUnit.DAYS.toSeconds(2)) {
                    startAfterString = mContext.getResources().getString(R.string.going_day_after_tomorrow);
                } else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(eventItem.date * 1000);
                    startAfterString = String.format("%s %s", calendar.get(Calendar.DAY_OF_MONTH), new DateFormatSymbols().getMonths()[calendar.get(Calendar.MONTH)].toLowerCase());
                }
            }
            TextView startItemView = eventCollectionItemViewHolder.getStartEventView();
            if (startAfterString == null) {
                startItemView.setVisibility(View.INVISIBLE);
            } else {
                startItemView.setVisibility(View.VISIBLE);
                startItemView.setText(startAfterString);
            }
            int categoryDrawableId = getCategoryDrawable(eventItem.category);
            if (categoryDrawableId != 0) {
                eventCollectionItemViewHolder.getCatalogIcon().setImageDrawable(mContext.getResources().getDrawable(categoryDrawableId));
            }
        } else {
            layoutParams.height = mContext.getResources().getDimensionPixelOffset(R.dimen.progress_wheel_size) + 2 * mMargin + 2 * mContext.getResources().getDimensionPixelOffset(R.dimen.event_loading_padding);
            layoutParams.setMargins(mMargin, mMargin, mMargin, mMargin);
        }

        viewHolder.itemView.setLayoutParams(layoutParams);
    }

    @Override
    public int getItemCount() {
        return mEventItems == null ? 0 : mEventItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).isLoadingItem) {
            return LOADING;
        }
        return NORMAL;
    }

    public WrapperEventItem getItem(int position) {
        if (position < 0 || mEventItems.size() <= position) return null;
        return mEventItems.get(position);
    }

    public void removeLoading() {
        if (getItemCount() <= 0) return;
        int position = getItemCount() - 1;
        WrapperEventItem item = mEventItems.get(position);
        if (item.isLoadingItem) {
            mEventItems.remove(position);
            notifyItemChanged(position);
        }
    }

    public void addLoading() {
        WrapperEventItem item = null;
        int position = getItemCount() - 1;
        if (getItemCount() != 0) {
            item = mEventItems.get(position);
        }

        if (getItemCount() == 0 || (item != null && !item.isLoadingItem)) {
            mEventItems.add(new WrapperEventItem(true));
            notifyItemChanged(position + 1);
        }
    }

    public boolean isLoading() {
        if (getItemCount() <= 0) return false;
        return getItemViewType(getItemCount() - 1) == LOADING;
    }

    public int getSpanSize(int position) {
        return getItemViewType(position) == LOADING ? mColumns : 1;
    }

    class WrapperEventItem {

        EventItem eventItem;
        boolean isLoadingItem = false;

        WrapperEventItem(EventItem eventItem) {
            this.eventItem = eventItem;
        }

        WrapperEventItem(boolean loading) {
            this.isLoadingItem = loading;
        }
    }

    class EventCollectionItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView getPlaceView() {
            return mPlaceView;
        }

        public ImageView getPosterThumbView() {
            return mPosterThumbView;
        }

        public ImageView getCatalogIcon() {
            return mCatalogIcon;
        }

        public TextView getNameView() {
            return mNameView;
        }

        public TextView getStartEventView() {
            return mStartItemView;
        }

        private TextView mPlaceView;
        private TextView mNameView;
        private TextView mStartItemView;
        private ImageView mPosterThumbView;
        private ImageView mCatalogIcon;

        public EventCollectionItemViewHolder(View view) {
            super(view);
            mPlaceView = (TextView) view.findViewById(R.id.place_view);
            mNameView = (TextView) view.findViewById(R.id.name_view);
            mPosterThumbView = (ImageView) view.findViewById(R.id.poster_thumb_view);
            mCatalogIcon = (ImageView) view.findViewById(R.id.category_view);
            mStartItemView = (TextView) view.findViewById(R.id.event_start_time_view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onEventItemSelect(getItem(getAdapterPosition()).eventItem);
        }
    }

    class LoadingHolder extends RecyclerView.ViewHolder {

        View itemView;

        public LoadingHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
        }

    }

}
