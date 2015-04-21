package ru.nekit.android.nowapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItemsLoader;
import ru.nekit.android.nowapp.utils.ConnectionUtil;

public class SplashScreenActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Integer> {

    private static final int LOADER_ID = 1;
    private ProgressWheel mProgressWheel;

    public SplashScreenActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mProgressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);
        if (ConnectionUtil.isInternetAvailable(this)) {
            NowApplication.setState(NowApplication.STATE.ONLINE);
            initFirstTimeLoader();
        } else {
            NowApplication.setState(NowApplication.STATE.OFFLINE);
            initFirstTimeLoader();
            //exitWitchError(R.string.connection_is_not_available);
        }
    }

    private void initFirstTimeLoader(){
        LoaderManager loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Integer> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            EventItemsLoader loader = new EventItemsLoader(this, args);
            loader.forceLoad();
            return loader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Integer> loader, Integer data) {
        if (data == 0) {
            mProgressWheel.stopSpinning();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(SplashScreenActivity.this, EventCollectionActivity.class));
                    finish();
                }
            }, 100);
        } else {
            exitWitchError(R.string.site_is_not_available);
        }
    }

    private void exitWitchError(final int errorDescriptionResourceId) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SplashScreenActivity.this, Html.fromHtml(getResources().getString(errorDescriptionResourceId)), Toast.LENGTH_LONG).show();
                finish();
            }
        }, 500);
    }

    @Override
    public void onLoaderReset(Loader<Integer> loader) {

    }
}
