package ru.nekit.android.nowapp.mvvm.view.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.pnikosis.materialishprogress.ProgressWheel;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.listeners.IFlayerDialogListener;

public class FlayerDialogFragment extends DialogFragment implements DialogInterface.OnShowListener {

    private static final String KEY_IMAGE_URL = "ru.nekit.android.image_url_key";
    private boolean mShowsDialog;
    private ProgressWheel mProgressWheel;
    private View mCloseButton;
    private String mImageUrl;
    private IFlayerDialogListener mDialogListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_flayer, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogFragment);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ImageView imageView = (ImageView) view.findViewById(R.id.image_view);
        mProgressWheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);
        mCloseButton = view.findViewById(R.id.close_button);
        mCloseButton.setOnClickListener(click -> {
            getDialog().dismiss();
        });

        Glide.with(this).load(mImageUrl).listener(new RequestListener<String, GlideDrawable>() {

            @Override
            public boolean onException(Exception exp, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                mProgressWheel.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                mProgressWheel.setVisibility(View.GONE);
                mCloseButton.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(imageView);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Bundle args = getArguments();
        mImageUrl = args.getString(KEY_IMAGE_URL);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnShowListener(this);
        return dialog;
    }

    private void setImageUrl(String imageUrl) {
        Bundle arg = getArguments();
        if (arg == null) {
            arg = new Bundle();
        }
        arg.putString(KEY_IMAGE_URL, imageUrl);
        setArguments(arg);
    }

    @NonNull
    public static FlayerDialogFragment getInstance(String imageUrl) {
        FlayerDialogFragment fragment = new FlayerDialogFragment();
        fragment.setImageUrl(imageUrl);
        return fragment;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        mShowsDialog = true;
    }

    public boolean isShowsDialog() {
        return mShowsDialog;
    }

    public void setDialogListener(EventDetailFragment dialogListener) {
        mDialogListener = dialogListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        getDialog().setOnShowListener(null);
        if (mDialogListener != null) {
            mDialogListener.onCloseDialog();
            mDialogListener = null;
        }
        mShowsDialog = false;
        super.onDismiss(dialog);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (mDialogListener != null) {
            mDialogListener.onShowDialog();
        }
    }
}