package ru.nekit.android.nowapp.view;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.pnikosis.materialishprogress.ProgressWheel;

import ru.nekit.android.nowapp.R;


public class EventPosterViewFragment extends Fragment {

    public static final String TAG = "ru.nekit.android.event_poster_view_fragment";

    private static final String IMAGE_URL = "ru.nekit.android.image_url";

    private static ImageLoader imageLoader;

    private ProgressWheel mProgressWheel;

    static {
        imageLoader = ImageLoader.getInstance();
    }

    private String mImageUrl;

    public void setEventPosterUrl(String imageUrl) {
        Bundle args = new Bundle();
        args.putString(IMAGE_URL, imageUrl);
        setArguments(args);
    }

    public EventPosterViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mImageUrl = getArguments().getString(IMAGE_URL);
        }
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setRetainInstance(false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poster_view, container, false);
        mProgressWheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);
        imageLoader.displayImage(mImageUrl, (ImageView) view.findViewById(R.id.poster_view), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                mProgressWheel.setVisibility(View.GONE);
            }
        });
        return view;
    }
}
