package ru.nekit.android.nowapp.mvvm.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.badoo.mobile.util.WeakHandler;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.OfflineDataStatus;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.databinding.FragmentSplashScreenBinding;
import ru.nekit.android.nowapp.model.loaders.EventLoader;
import ru.nekit.android.nowapp.mvvm.view.activities.EventCollectionActivity;
import ru.nekit.android.nowapp.mvvm.viewModel.SplashScreenViewModel;
import rx.subscriptions.CompositeSubscription;

public class SplashScreenFragment extends Fragment implements LoaderManager.LoaderCallbacks<Integer> {

    private final WeakHandler mHandler;
    private final SplashScreenViewModel mViewModel;
    private final CompositeSubscription mSubscriptions;

    private FragmentSplashScreenBinding mBinding;
    private LayoutInflater mInflater;

    public SplashScreenFragment() {
        mHandler = new WeakHandler();

        //create view model
        mViewModel = new SplashScreenViewModel();

        //list of subscriptions
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void initFirstTimeLoader() {
        getActivity().getSupportLoaderManager().initLoader(0, new Bundle(), this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInflater = inflater;

        //binding
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_splash_screen, container, false);

        //init view model
        mViewModel.init();

        //subscribe on connectivity problem
        mSubscriptions.add(mViewModel.onConnectivityProblem().subscribe(value -> showConnectivityProblemDialog()));

        //initFirstTimeLoader();

        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void showConnectivityProblemDialog() {
        AlertDialog.Builder builder;
        AppCompatDialog dialog;
        Context context = getActivity();

        View dialogContentView = mInflater.inflate(R.layout.dialog_content, null, false);
        View dialogTitleView = mInflater.inflate(R.layout.dialog_title, null, false);
        TextView dialogTextView = (TextView) dialogContentView.findViewById(R.id.text_view);
        dialogTextView.setTextAppearance(context, R.style.DialogContent);
        TextView dialogTitleTextView = (TextView) dialogTitleView.findViewById(R.id.text_view);
        dialogTitleTextView.setTextAppearance(context, R.style.DialogTitle);
        builder = new AlertDialog.Builder(context, R.style.DialogTheme);

        mBinding.progressWheel.setVisibility(View.GONE);
        OfflineDataStatus status = NowApplication.getInstance().getOfflineStatus();
        switch (status) {

            case IS_UP_TO_DATE:

                builder.setNegativeButton(R.string.no, (dialog1, which) -> {
                    exit();
                });
                builder.setPositiveButton(R.string.yes, (dialog1, which) -> {
                    mBinding.progressWheel.setVisibility(View.VISIBLE);
                    initFirstTimeLoader();
                });
                builder.setCancelable(false)
                        .setView(dialogContentView)
                        .setCustomTitle(dialogTitleView);

                dialogTextView.setText(R.string.offline_state_dialog_text);
                dialogTitleTextView.setText(R.string.offline_state_dialog_title);

                break;

            case IS_OUT_OF_DATE:
            case UNKNOWN:

                builder.setNegativeButton(R.string.exit, (dialog1, which) -> {
                    exit();
                });
                builder.setCancelable(false)
                        .setView(dialogContentView)
                        .setCustomTitle(dialogTitleView);

                dialogTextView.setText(String.format(getResources().getString(R.string.default_state_dialog_text), getResources().getString(R.string.exit)));
                dialogTitleTextView.setText(R.string.default_state_dialog_title);

                break;
        }

        dialog = builder.create();
        dialog.show();
    }

    @Nullable
    @Override
    public Loader<Integer> onCreateLoader(int id, Bundle args) {
        Context context = NowApplication.getInstance();
        EventLoader loader = new EventLoader(context, args);
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Integer> loader, Integer data) {
        if (data == 0) {
            mBinding.progressWheel.stopSpinning();
            mHandler.postDelayed(() -> {
                NowApplication.getInstance().checkForUpdate();
                startActivity(new Intent(getActivity(), EventCollectionActivity.class));
                exit();
            }, 100);
        } else {
            //showConnectivityProblemDialog();
        }

    }

    private void exit() {
        getActivity().finish();
    }

    @Override
    public void onLoaderReset(Loader<Integer> loader) {

    }
}
