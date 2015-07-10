package ru.nekit.android.nowapp.modelView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventsModel;
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.modelView.listeners.IEventClickListener;

import static ru.nekit.android.nowapp.model.EventsModel.getCategoryDrawable;
import static ru.nekit.android.nowapp.model.EventsModel.getCurrentDateTime;

/**
 * Created by chuvac on 12.03.15.
 */
public class EventCollectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private static final int NORMAL = 0, LOADING = 1;
    private static final boolean STOP_IMAGE_LOADING_WITH_QUICK_SCROLLING = true;
    private static final int MAX_SCROLL_SPEED = 70;

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final ArrayList<EventItemWrapper> mEventItems;
    private final ArrayList<Target> mLoadingList;
    private final EventsModel mEventModel;
    private final int mItemHeight, mColumns, mMargin, mLoadMoreCount;
    private WeakReference<RecyclerView> mRecyclerViewReference;
    private RecyclerView.OnScrollListener mScrollListener;
    private IEventClickListener mItemClickListener;
    private boolean mImmediateImageLoading;
    private OnLoadMorelListener mLoadMoreListener;
    private EventItemWrapper mLoadingItem;
    private BroadcastReceiver mUpdateReceiver;

    public EventCollectionAdapter(Context context, int columns) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mColumns = columns;
        int screenWidth = getScreenWidth(context);
        TypedValue outValue = new TypedValue();
        context.getResources().getValue(R.dimen.event_item_view_height_ratio, outValue, true);
        mItemHeight = (int) (((screenWidth - mContext.getResources().getDimensionPixelOffset(R.dimen.event_collection_padding) * 2 - mContext.getResources().getDimensionPixelOffset(R.dimen.event_collection_space) * (columns - 1)) / columns) * (columns > 1 ? outValue.getFloat() : .6));
        mMargin = context.getResources().getDimensionPixelSize(R.dimen.event_collection_space);
        mEventItems = new ArrayList<>();
        mLoadingList = new ArrayList<>();
        mEventModel = EventsModel.getInstance(context);
        mImmediateImageLoading = true;
        mLoadMoreCount = columns > 2 ? 0 : columns;
    }

    private static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    public void setOnItemClickListener(IEventClickListener listener) {
        mItemClickListener = listener;
    }

    public void registerRecyclerView(RecyclerView recyclerView) {
        mRecyclerViewReference = new WeakReference<>(recyclerView);
        mScrollListener = new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    if (mEventModel.isAvailableLoad()) {
                        if (totalItemCount > 1) {
                            if (lastVisibleItem >= totalItemCount - 1 - mLoadMoreCount) {
                                if (mLoadMoreListener != null) {
                                    mLoadMoreListener.onLoadMore();
                                }
                            }
                        }
                    }
                    continueImageLoading(firstVisibleItem, lastVisibleItem);
                }
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (STOP_IMAGE_LOADING_WITH_QUICK_SCROLLING && Math.abs(dy) > MAX_SCROLL_SPEED) {
                    stopImageLoading();
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
        mEventModel.registerForFiveMinuteUpdateNotification(mUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateEventStartTime();
            }
        });
        updateEventStartTime();
    }

    private void updateEventStartTime() {
        RecyclerView recyclerView = mRecyclerViewReference.get();
        if (recyclerView != null) {
            GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null) {
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                if (firstVisibleItem > -1 && lastVisibleItem > -1) {
                    for (int i = firstVisibleItem; i <= lastVisibleItem; i++) {
                        EventCollectionItemViewHolder eventCollectionItemViewHolder = (EventCollectionItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                        if (eventCollectionItemViewHolder != null) {
                            EventItemWrapper eventItemWrapper = getItem(i);
                            if (eventItemWrapper != null) {
                                setStartTimeForEvent(eventItemWrapper.event, eventCollectionItemViewHolder);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setLoadMoreListener(OnLoadMorelListener listener) {
        mLoadMoreListener = listener;
    }

    public void unregisterRecyclerView(RecyclerView recyclerView) {
        if (mScrollListener != null) {
            recyclerView.removeOnScrollListener(mScrollListener);
        }
        mEventItems.clear();
        mScrollListener = null;
        mEventModel.unregisterForFiveMinuteUpdateNotification(mUpdateReceiver);
    }

    public void addItems(ArrayList<Event> events) {
        int fistItemIndex = mEventItems.size();
        boolean hasLoading = false;
        if (isLoading()) {
            hasLoading = true;
            fistItemIndex--;
            removeLoading();
        }
        if (setItems(events, true)) {
            for (int i = fistItemIndex; i < mEventItems.size(); i++) {
                notifyItemInserted(i);
            }
        }
        if (hasLoading) {
            addLoading();
        }
    }

    private boolean setItems(ArrayList<Event> events, boolean addState) {
        if (!addState) {
            mEventItems.clear();
        }
        if (events != null && events.size() > 0) {
            for (Event event : events) {
                long dateDelta = event.date - getCurrentDateTime(mContext, true);
                if (dateDelta >= 0 && event.endAt > EventsModel.getCurrentDayTime(mContext, true) || dateDelta > 1) {
                    mEventItems.add(new EventItemWrapper(event));
                }
            }
            if (!addState) {
                notifyDataSetChanged();
            }
            return true;
        } else {
            notifyDataSetChanged();
        }
        return false;
    }

    public boolean setItems(ArrayList<Event> events) {

        return setItems(events, false);
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
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        final GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
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
            final EventCollectionItemViewHolder eventCollectionItemViewHolder = (EventCollectionItemViewHolder) viewHolder;
            final EventItemWrapper eventItemWrapper = mEventItems.get(position);
            final Event event = eventItemWrapper.event;
            final String eventName = event.name.toUpperCase();
            if (eventItemWrapper.cachedName == null) {
                ArrayList<String> eventNameArray = ru.nekit.android.nowapp.utils.StringUtil.wrapText(eventName);
                eventItemWrapper.cachedNameLineCount = eventNameArray.size();
                eventItemWrapper.cachedName = TextUtils.join("\n", eventNameArray);
            }
            final TextView eventNameView = eventCollectionItemViewHolder.getNameView();
            eventCollectionItemViewHolder.getPlaceView().setText(event.placeName);
            if (eventNameView.getLineCount() != eventItemWrapper.cachedNameLineCount) {
                eventNameView.setLines(eventItemWrapper.cachedNameLineCount);
            }
            eventNameView.setText(eventItemWrapper.cachedName);
            eventItemWrapper.posterLoaded = false;
            if (mImmediateImageLoading) {
                addToLoadingList(eventItemWrapper, eventCollectionItemViewHolder.getPosterThumbView());
            } else {
                eventCollectionItemViewHolder.getPosterThumbView().setImageDrawable(null);
            }
            setStartTimeForEvent(event, eventCollectionItemViewHolder);
            int categoryDrawableId = getCategoryDrawable(event.category);
            if (categoryDrawableId != 0) {
                ImageView imageView = eventCollectionItemViewHolder.getCatalogIcon();
                imageView.setImageDrawable(mContext.getResources().getDrawable(categoryDrawableId));
            }
        } else {
            layoutParams.height = mContext.getResources().getDimensionPixelOffset(R.dimen.progress_wheel_size) + 2 * (mMargin + mContext.getResources().getDimensionPixelOffset(R.dimen.event_loading_padding));
            layoutParams.setMargins(mMargin, mMargin, mMargin, mMargin);
        }
    }

    private void setStartTimeForEvent(Event event, EventCollectionItemViewHolder eventCollectionItemViewHolder) {
        String startTimeAliasString = EventsModel.getStartTimeAlias(mContext, event);
        TextView startItemView = eventCollectionItemViewHolder.getStartEventView();
        if (startTimeAliasString == null) {
            startItemView.setVisibility(View.INVISIBLE);
        } else {
            startItemView.setVisibility(View.VISIBLE);
            startItemView.setText(startTimeAliasString);
        }
    }

    @Override
    public long getItemId(int position) {
        EventItemWrapper eventItemWrapper = getItem(position);
        return getItemViewType(position) == LOADING ? -1 : eventItemWrapper == null ? -1 : eventItemWrapper.event.id;
    }

    private void addToLoadingList(final EventItemWrapper eventItemWrapper, final ImageView viewTarget) {
        mLoadingList.add(Glide.with(mContext).load(eventItemWrapper.event.posterBlur).centerCrop().dontAnimate().listener(new RequestListener<String, GlideDrawable>() {
            @Override
            public boolean onException(Exception exp, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                return true;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                mLoadingList.remove(target);
                eventItemWrapper.posterLoaded = true;
                viewTarget.setColorFilter(mContext.getResources().getColor(R.color.poster_overlay), PorterDuff.Mode.MULTIPLY);
                return false;
            }
        }).into(viewTarget));
    }

    private void resetCurrentLoadingList() {
        if (mLoadingList.size() > 0) {
            for (Target target : mLoadingList) {
                if (target.getRequest() != null && target.getRequest().isRunning()) {
                    Glide.clear(target);
                }
            }
            mLoadingList.clear();
        }
    }

    @Override
    public int getItemCount() {
        return mEventItems == null ? 0 : mEventItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        EventItemWrapper item = getItem(position);
        if (item != null && item.isLoadingItem) {
            return LOADING;
        }
        return NORMAL;
    }

    private EventItemWrapper getItem(int position) {
        if (position < 0 || mEventItems.size() <= position) return null;
        return mEventItems.get(position);
    }

    public void removeLoading() {
        if (getItemCount() <= 0) return;
        if (mLoadingItem != null) {
            int position = getItemCount();
            mEventItems.remove(position - 1);
            notifyItemRemoved(position);
            mLoadingItem = null;
        }
    }

    public void addLoading() {
        if (mLoadingItem == null) {
            int position = getItemCount();
            mEventItems.add(mLoadingItem = new EventItemWrapper(true));
            notifyItemInserted(position);
        }
    }

    public boolean isLoading() {
        return mLoadingItem != null;
    }

    public int getSpanSize(int position) {
        return getItemViewType(position) == LOADING ? mColumns : 1;
    }

    private void stopImageLoading() {
        if (mImmediateImageLoading) {
            mImmediateImageLoading = false;
            resetCurrentLoadingList();
        }
    }

    private void continueImageLoading(int firstVisibleItem, int lastVisibleItem) {
        if (!mImmediateImageLoading) {
            mImmediateImageLoading = true;
            for (int i = firstVisibleItem; i <= lastVisibleItem; i++) {
                final RecyclerView recyclerView = mRecyclerViewReference.get();
                if (recyclerView != null) {
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                    if (viewHolder.getClass() == EventCollectionItemViewHolder.class) {
                        EventCollectionItemViewHolder eventCollectionItemViewHolder = (EventCollectionItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                        EventItemWrapper eventItemWrapper = getItem(i);
                        if (eventItemWrapper != null && !eventItemWrapper.posterLoaded) {
                            addToLoadingList(getItem(i), eventCollectionItemViewHolder.getPosterThumbView());
                        }
                    }
                }
            }
        }
    }

    public interface OnLoadMorelListener {
        void onLoadMore();
    }

    class EventItemWrapper {

        public String cachedName;
        public int cachedNameLineCount;
        public boolean posterLoaded;
        private Event event;
        private boolean isLoadingItem = false;

        EventItemWrapper(Event event) {
            cachedName = null;
            cachedNameLineCount = 0;
            posterLoaded = false;
            this.event = event;
        }

        EventItemWrapper(boolean loading) {
            this.isLoadingItem = loading;
        }
    }

    class EventCollectionItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mPlaceView;
        private TextView mNameView;
        private TextView mStartItemView;
        private ImageView mPosterThumbView;
        private ImageView mCatalogIcon;

        public EventCollectionItemViewHolder(View view) {
            super(view);
            mPlaceView = (TextView) view.findViewById(R.id.place_name_view);
            mNameView = (TextView) view.findViewById(R.id.name_view);
            mPosterThumbView = (ImageView) view.findViewById(R.id.poster_thumb_view);
            mCatalogIcon = (ImageView) view.findViewById(R.id.category_view);
            mStartItemView = (TextView) view.findViewById(R.id.event_start_time_view);
            view.setOnClickListener(this);
        }

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

        @Override
        public void onClick(View view) {
            EventItemWrapper eventItemWrapper = getItem(getAdapterPosition());
            if (eventItemWrapper != null) {
                mItemClickListener.onEventClick(eventItemWrapper.event, false);
            }
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
