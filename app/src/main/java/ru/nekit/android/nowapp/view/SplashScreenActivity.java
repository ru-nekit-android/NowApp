package ru.nekit.android.nowapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.devspark.robototextview.util.RobotoTypefaceManager;
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
            showConnectivityProblemDialog();
        }
    }

    private void showConnectivityProblemDialog() {
        mProgressWheel.setVisibility(View.GONE);
        if (NowApplication.offlineAllow()) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.dialog_title_offline_state)
                    .content(R.string.dialog_content_offline_state)
                    .positiveText(android.R.string.yes)
                    .negativeText(R.string.no)
                    .typeface(RobotoTypefaceManager.obtainTypeface(getApplicationContext(), RobotoTypefaceManager.Typeface.ROBOTO_REGULAR),
                            RobotoTypefaceManager.obtainTypeface(getApplicationContext(), RobotoTypefaceManager.Typeface.ROBOTO_REGULAR))
                    .customView(R.layout.connectivity_content, false)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            initFirstTimeLoader();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            finish();
                        }
                    })
                    .disableDefaultFonts()
                    .cancelable(false)
                    .show();

            View view = dialog.getCustomView();
            ((TextView) view.findViewById(R.id.text_view)).setText(R.string.dialog_content_offline_state);
        } else {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.dialog_title_default_state)
                    .negativeText(R.string.exit)
                    .typeface(RobotoTypefaceManager.obtainTypeface(getApplicationContext(), RobotoTypefaceManager.Typeface.ROBOTO_REGULAR),
                            RobotoTypefaceManager.obtainTypeface(getApplicationContext(), RobotoTypefaceManager.Typeface.ROBOTO_REGULAR))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            finish();
                        }
                    })
                    .customView(R.layout.connectivity_content, false)
                    .disableDefaultFonts()
                    .cancelable(false)
                    .show();

            View view = dialog.getCustomView();
            ((TextView) view.findViewById(R.id.text_view)).setText(String.format(getResources().getString(R.string.dialog_content_default_state), getResources().getString(R.string.exit)));
        }
    }

    private void initFirstTimeLoader() {
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
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
            showConnectivityProblemDialog();
        }
    }

    @Override
    public void onLoaderReset(Loader<Integer> loader) {

    }
}
