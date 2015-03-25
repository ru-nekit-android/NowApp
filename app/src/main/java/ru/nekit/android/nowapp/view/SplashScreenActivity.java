package ru.nekit.android.nowapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;

import com.pnikosis.materialishprogress.ProgressWheel;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItemsLoader;

public class SplashScreenActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Void> {

    private static final int LOADER_ID = 1;
    private ProgressWheel mProgressWheel;

    public SplashScreenActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        LoaderManager loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(LOADER_ID, null, this);
        mProgressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);
    }

    @Override
    public Loader<Void> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            EventItemsLoader loader = new EventItemsLoader(this, args);
            loader.forceLoad();
            return loader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Void> loader, Void data) {
        mProgressWheel.stopSpinning();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this, EventCollectionActivity.class));
                finish();
            }
        }, 100);
    }

    @Override
    public void onLoaderReset(Loader<Void> loader) {

    }
}
