package ru.nekit.android.nowapp.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.view.View;
import android.widget.TextView;

import com.badoo.mobile.util.WeakHandler;
import com.pnikosis.materialishprogress.ProgressWheel;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventLoader;
import ru.nekit.android.nowapp.utils.ConnectionUtil;

import static ru.nekit.android.nowapp.NowApplication.APP_STATE;
import static ru.nekit.android.nowapp.NowApplication.getOfflineState;
import static ru.nekit.android.nowapp.NowApplication.setState;

public class SplashScreenActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Integer> {

    private static final int LOADER_ID = 1;
    private ProgressWheel mProgressWheel;
    private final WeakHandler mHandler;

    public SplashScreenActivity() {
        mHandler = new WeakHandler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mProgressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);
        if (ConnectionUtil.isInternetAvailable(this)) {
            setState(APP_STATE.ONLINE);
            initFirstTimeLoader();
        } else {
            setState(APP_STATE.OFFLINE);
            showConnectivityProblemDialog();
        }
    }

    private void showConnectivityProblemDialog() {
        AlertDialog.Builder builder;
        AppCompatDialog dialog;
        View dialogContentView = getLayoutInflater().inflate(R.layout.dialog_content, null, false);
        View dialogTitleView = getLayoutInflater().inflate(R.layout.dialog_title, null, false);
        TextView dialogTextView = (TextView) dialogContentView.findViewById(R.id.text_view);
        dialogTextView.setTextAppearance(this, R.style.DialogContent);
        TextView dialogTitleTextView = (TextView) dialogTitleView.findViewById(R.id.text_view);
        dialogTitleTextView.setTextAppearance(this, R.style.DialogTitle);
        builder = new AlertDialog.Builder(this, R.style.DialogTheme);

        mProgressWheel.setVisibility(View.GONE);
        switch (getOfflineState()) {

            case DATA_IS_UP_TO_DATE:

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressWheel.setVisibility(View.VISIBLE);
                        initFirstTimeLoader();
                    }
                }).setCancelable(false)
                        .setView(dialogContentView)
                        .setCustomTitle(dialogTitleView);

                dialogTextView.setText(R.string.offline_state_dialog_text);
                dialogTitleTextView.setText(R.string.offline_state_dialog_title);

                break;

            case DATA_IS_OUT_OF_DATE:
            case DATA_IS_EMPTY:

                builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false)
                        .setView(dialogContentView)
                        .setCustomTitle(dialogTitleView);
                dialogTextView.setText(String.format(getResources().getString(R.string.default_state_dialog_text), getResources().getString(R.string.exit)));
                dialogTitleTextView.setText(R.string.default_state_dialog_title);

                break;
        }

        dialog = builder.create();
        dialog.show();
    }

    private void initFirstTimeLoader() {
        getSupportLoaderManager().initLoader(LOADER_ID, new Bundle(), this);
    }

    @Nullable
    @Override
    public Loader<Integer> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID) {
            EventLoader loader = new EventLoader(this, args);
            loader.forceLoad();
            return loader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Integer> loader, Integer data) {
        if (data == 0) {
            mProgressWheel.stopSpinning();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    NowApplication.checkForUpdate();
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
