package ru.nekit.android.nowapp.mvvm.view.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.mvvm.view.fragments.EventCollectionFragment;
import ru.nekit.android.nowapp.mvvm.view.fragments.EventDetailFragment;
import ru.nekit.android.nowapp.mvvm.view.fragments.EventPosterViewFragment;
import ru.nekit.android.nowapp.listeners.IBackPressedListener;
import ru.nekit.android.nowapp.listeners.IEventClickListener;
import ru.nekit.android.nowapp.listeners.IEventPosterSelectListener;
import ru.nekit.android.nowapp.model.vo.Event;

public class EventCollectionActivity extends AppCompatActivity implements IEventClickListener, IEventPosterSelectListener {

    private BroadcastReceiver mChangeApplicationStateReceiver;
    private View mOfflineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_collection);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_action_previous_item);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        EventCollectionFragment mEventCollectionFragment = new EventCollectionFragment();
        if (fragmentManager.findFragmentByTag(EventCollectionFragment.TAG) == null) {
            fragmentManager.beginTransaction().add(R.id.event_place_holder, mEventCollectionFragment, EventCollectionFragment.TAG).commit();
        }

        mOfflineView = findViewById(R.id.offline_view);
        mChangeApplicationStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateOfflineView();
            }
        };
        updateOfflineView();

    }

    private void updateOfflineView() {
        if (NowApplication.getInstance().getState() == NowApplication.AppState.ONLINE) {
            mOfflineView.setVisibility(View.GONE);
        } else {
            mOfflineView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Fragment fragment = getCurrentFragment();
            if (fragment != null && fragment.isVisible() && fragment instanceof IBackPressedListener) {
                ((IBackPressedListener) fragment).onBackPressed();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private Fragment getCurrentFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        return fragmentManager.findFragmentById(R.id.event_place_holder);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:

                Fragment fragment = getCurrentFragment();
                if (fragment != null && fragment.isVisible()) {
                    getSupportFragmentManager().popBackStack();
                }

                return true;

            case R.id.action_about:

                AlertDialog.Builder builder;
                AppCompatDialog dialog;
                View dialogContentView = getLayoutInflater().inflate(R.layout.view_about, null, true);
                View dialogTitleView = getLayoutInflater().inflate(R.layout.dialog_title, null, true);
                TextView dialogTextView = (TextView) dialogContentView.findViewById(R.id.text_view);
                dialogTextView.setTextAppearance(this, R.style.DialogContent);
                TextView dialogTitleTextView = (TextView) dialogTitleView.findViewById(R.id.text_view);
                dialogTitleTextView.setTextAppearance(this, R.style.DialogTitle);
                builder = new AlertDialog.Builder(this, R.style.DialogTheme);
                builder.setCancelable(true)
                        .setView(dialogContentView)
                        .setCustomTitle(dialogTitleView);
                builder.setInverseBackgroundForced(true);
                dialogTitleTextView.setText(R.string.about_title);
                dialogTextView.setMovementMethod(LinkMovementMethod.getInstance());
                dialog = builder.create();
                dialog.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onEventClick(Event event, boolean openNew) {
        EventDetailFragment fragment;
        fragment = EventDetailFragment.getInstance();
        fragment.setEventAndAdvertPossibility(event, !openNew);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).replace(R.id.event_place_holder, fragment, EventDetailFragment.TAG).addToBackStack(null).commit();
    }

    @Override
    public void onEventItemPosterSelect(String posterUrl) {
        EventPosterViewFragment fragment = EventPosterViewFragment.getInstance(posterUrl);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).replace(R.id.event_place_holder, fragment, EventPosterViewFragment.TAG).addToBackStack(null).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NowApplication.getInstance().registerForAppChangeStateNotification(mChangeApplicationStateReceiver);
        NowApplication.getInstance().setConnectionReceiverActive(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        NowApplication.getInstance().unregisterForAppChangeStateNotification(mChangeApplicationStateReceiver);
        NowApplication.getInstance().setConnectionReceiverActive(false);
    }

}