package ru.nekit.android.nowapp.modelView;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.model.EventItemsModel;

/**
 * Created by chuvac on 12.03.15.
 */
public class EventCollectionAdapter extends RecyclerView.Adapter<EventCollectionItemViewHolder> {

    private final LayoutInflater mInflater;
    private final Context mContext;

    private ArrayList<EventItem> mEventItems;

    public EventCollectionAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    public void add(int position, EventItem item) {
        mEventItems.add(position, item);
        notifyDataSetChanged();
    }

    public void remove(EventItem item) {
        int position = mEventItems.indexOf(item);
        mEventItems.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public EventCollectionItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_event_collection, parent, false);
        EventCollectionItemViewHolder viewHolder = new EventCollectionItemViewHolder(view);
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) view.getLayoutParams();
        layoutParams.height = mContext.getResources().getDimensionPixelSize(R.dimen.event_collection_item_height) + mContext.getResources().getDimensionPixelSize(R.dimen.event_collection_list_spacing);
        view.setLayoutParams(layoutParams);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(EventCollectionItemViewHolder holder, int position) {
        final EventItem event = mEventItems.get(position);
        holder.getPlaceView().setText(event.placeName);
        holder.getNameView().setText(event.name);
        Glide.with(mContext).load(event.posterBlur).centerCrop().into(holder.getPosterView());
        int categoryDrawableId = EventItemsModel.CATALOG_TYPE.get(event.category);
        if (categoryDrawableId != 0) {
            holder.getCatalogIcon().setImageDrawable(mContext.getResources().getDrawable(categoryDrawableId));
        }
    }

    @Override
    public int getItemCount() {
        return mEventItems == null ? 0 : mEventItems.size();
    }


    public void setEventItems(ArrayList<EventItem> items) {
        mEventItems = items;
        notifyDataSetChanged();
    }
}
