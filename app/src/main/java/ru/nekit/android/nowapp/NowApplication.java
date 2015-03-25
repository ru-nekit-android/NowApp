package ru.nekit.android.nowapp;

import android.app.Application;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import ru.nekit.android.nowapp.model.EventItemsModel;

/**
 * Created by chuvac on 17.03.15.
 */
public class NowApplication extends Application {

    private EventItemsModel mEventModel;

    public NowApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mEventModel = EventItemsModel.getInstance();

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheOnDisk(true).cacheInMemory(true)
                .imageScaleType(ImageScaleType.NONE_SAFE)
                .displayer(new FadeInBitmapDisplayer(500)).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                getApplicationContext())
                .threadPoolSize(2)
                .threadPriority(Thread.MIN_PRIORITY + 2)
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCache(new WeakMemoryCache())
                .diskCacheSize(100 * 1024 * 1024).build();

        ImageLoader.getInstance().init(config);
    }

    public EventItemsModel getEventModel() {
        return mEventModel;
    }

}
