package ru.nekit.android.nowapp.modelView;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ru.nekit.android.nowapp.R;

/**
 * Created by chuvac on 12.03.15.
 */
public class EventCollectionItemViewHolder extends RecyclerView.ViewHolder {


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
        mCatalogIcon = (ImageView) view.findViewById(R.id.category_type_view);
    }
}

