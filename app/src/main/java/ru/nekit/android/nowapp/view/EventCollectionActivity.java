package ru.nekit.android.nowapp.view;

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
import ru.nekit.android.nowapp.model.vo.Event;
import ru.nekit.android.nowapp.modelView.listeners.IBackPressedListener;
import ru.nekit.android.nowapp.modelView.listeners.IEventClickListener;
import ru.nekit.android.nowapp.modelView.listeners.IEventPosterSelectListener;


public class EventCollectionActivity extends AppCompatActivity implements IEventClickListener, IEventPosterSelectListener {

    private EventPosterViewFragment mEventPosterViewFragment;
    private BroadcastReceiver mChangeApplicationStateReceiver;
    private View mOfflineView;
    private EventDetailFragment mEventDetailFragment;

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
        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
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
                View dialogContentView = getLayoutInflater().inflate(R.layout.about_view, null, true);
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
        if (openNew) {
            fragment = EventDetailFragment.getInstance();
        } else {
            if (mEventDetailFragment == null) {
                mEventDetailFragment = EventDetailFragment.getInstance();
            }
            fragment = mEventDetailFragment;
        }
        fragment.setEventAndAdvertPossibility(event, !openNew);
        if (!fragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).replace(R.id.event_place_holder, fragment, EventDetailFragment.TAG).addToBackStack(null).commit();
        }
    }

    @Override
    public void onEventItemPosterSelect(String posterUrl) {
        if (mEventPosterViewFragment == null) {
            mEventPosterViewFragment = EventPosterViewFragment.getInstance(posterUrl);
        } else {
            mEventPosterViewFragment.updateEventPosterUrl(posterUrl);
        }
        if (!mEventPosterViewFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).replace(R.id.event_place_holder, mEventPosterViewFragment, EventPosterViewFragment.TAG).addToBackStack(null).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NowApplication.registerForAppChangeStateNotification(mChangeApplicationStateReceiver);
        NowApplication.setConnectionReceiverActive(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        NowApplication.unregisterForAppChangeStateNotification(mChangeApplicationStateReceiver);
        NowApplication.setConnectionReceiverActive(false);
    }

}
