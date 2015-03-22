package ru.nekit.android.nowapp.modelView;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsModel;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemSelectListener;

/**
 * Created by chuvac on 12.03.15.
 */
public class EventCollectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    final int NORMAL = 0, LOADING = 1;

    private int mItemWidth, mItemHeight, mColumns, mMargin;
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
        mItemWidth = (screenWidth / columns);
        mItemHeight = (int) (mItemWidth * .8);
        mMargin = context.getResources().getDimensionPixelSize(R.dimen.event_collection_space);
        setItems(items);
    }

    public void setItems(ArrayList<EventItem> items) {
        mEventItems.clear();
        if (null != items) {
            for (EventItem item : items) {
                mEventItems.add(new WrapperEventItem(item));
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
        try {
            switch (viewType) {
                case LOADING:
                    view = mInflater.inflate(R.layout.item_event_loading, parent, false);
                    return new LoadingHolder(view, mItemHeight);
                case NORMAL:
                default:
                    view = mInflater.inflate(R.layout.item_event, parent, false);
                    return new EventCollectionItemViewHolder(view);
            }
        }catch(Exception exp){
            Log.e("ru.n.a", exp.getMessage());
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
        if (getItemViewType(position) == NORMAL) {
            layoutParams.height = mItemHeight;
            layoutParams.width = mItemWidth;
            if (position % mColumns == 0) {
                layoutParams.setMargins(mMargin, mMargin, mMargin / 2, 0);
            } else {
                layoutParams.setMargins(mMargin / 2, mMargin, mMargin, 0);
            }
            EventCollectionItemViewHolder eventCollectionItemViewHolder = (EventCollectionItemViewHolder) viewHolder;
            final EventItem event = mEventItems.get(position).eventItem;
            eventCollectionItemViewHolder.getPlaceView().setText(event.placeName);
            eventCollectionItemViewHolder.getNameView().setText(event.name);
            Glide.with(mContext).load(event.posterBlur).centerCrop().into(eventCollectionItemViewHolder.getPosterView());

            int categoryDrawableId = EventItemsModel.getCategoryDrawable(event.category);
            if (categoryDrawableId != 0) {
                eventCollectionItemViewHolder.getCatalogIcon().setImageDrawable(mContext.getResources().getDrawable(categoryDrawableId));
            }
        } else {
            layoutParams.height = mItemWidth / 4;
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
        WrapperEventItem item = mEventItems.get(getItemCount() - 1);
        if (item.isLoadingItem) {
            mEventItems.remove(getItemCount() - 1);
            notifyDataSetChanged();
        }
    }

    public void addLoading() {
        WrapperEventItem item = null;
        if (getItemCount() != 0) {
            item = mEventItems.get(getItemCount() - 1);
        }

        if (getItemCount() == 0 || (item != null && !item.isLoadingItem)) {
            mEventItems.add(new WrapperEventItem(true));
            notifyDataSetChanged();
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

        public ImageView getPosterView() {
            return mPosterView;
        }

        public ImageView getCatalogIcon() {
            return mCatalogIcon;
        }

        public TextView getNameView() {
            return mNameView;
        }

        private TextView mPlaceView;
        private TextView mNameView;
        private ImageView mPosterView;
        private ImageView mCatalogIcon;

        public EventCollectionItemViewHolder(View view) {
            super(view);
            mPlaceView = (TextView) view.findViewById(R.id.place_view);
            mNameView = (TextView) view.findViewById(R.id.name_view);
            mPosterView = (ImageView) view.findViewById(R.id.poster_thumb_view);
            mCatalogIcon = (ImageView) view.findViewById(R.id.category_view);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onEventItemSelect(getItem(getAdapterPosition()).eventItem);
        }
    }

    class LoadingHolder extends RecyclerView.ViewHolder {

        View itemView;

        public LoadingHolder(View itemView, int itemHeight) {
            super(itemView);
            this.itemView = itemView;
            itemView.setMinimumHeight(itemHeight);
        }

    }

}
