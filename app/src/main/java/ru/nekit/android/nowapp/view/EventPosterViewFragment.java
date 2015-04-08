package ru.nekit.android.nowapp.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.pnikosis.materialishprogress.ProgressWheel;

import ru.nekit.android.nowapp.R;

public class EventPosterViewFragment extends Fragment {

    public static final String TAG = "ru.nekit.android.event_poster_view_fragment";

    private static final String IMAGE_URL = "ru.nekit.android.image_url";

    private ProgressWheel mProgressWheel;

    private String mImageUrl;

    private void setEventPosterUrl(String imageUrl) {
        Bundle args = new Bundle();
        args.putString(IMAGE_URL, imageUrl);
        setArguments(args);
    }

    public void updateEventPosterUrl(String imageUrl) {
        Bundle args = getArguments();
        args.putString(IMAGE_URL, imageUrl);
    }

    public EventPosterViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mImageUrl = getArguments().getString(IMAGE_URL);
        }
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poster_view, container, false);
        mProgressWheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);
        Glide.with(getActivity()).load(mImageUrl).listener(new RequestListener<String, GlideDrawable>() {

            @Override
            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                mProgressWheel.setVisibility(View.GONE);
                return false;
            }
        }).into((ImageView) view.findViewById(R.id.poster_view));
        return view;
    }

    public static EventPosterViewFragment getInstance(String posterUrl) {
        EventPosterViewFragment fragment = new EventPosterViewFragment();
        fragment.setEventPosterUrl(posterUrl);
        return fragment;
    }
}
