package ru.nekit.android.nowapp.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.devspark.robototextview.util.RobotoTypefaceManager;

import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemPosterSelectListener;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemSelectListener;


public class EventCollectionActivity extends ActionBarActivity implements IEventItemSelectListener, IEventItemPosterSelectListener {

    private EventCollectionFragment mEventCollectionFragment;
    private EventDetailFragment mEventDetailFragment;
    private EventPosterViewFragment mEventPosterViewFragment;
    private BroadcastReceiver mChangeApplicationStateReceiver;
    private View mOfflineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_collection);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_action_previous_item);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        FragmentManager fragmentManager = getSupportFragmentManager();
        mEventCollectionFragment = new EventCollectionFragment();
        if (fragmentManager.findFragmentByTag(EventCollectionFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.event_place_holder, mEventCollectionFragment, EventCollectionFragment.TAG).commit();
        }

        mOfflineView = findViewById(R.id.offline_view);
        mChangeApplicationStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateOfflineView();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mChangeApplicationStateReceiver, new IntentFilter(NowApplication.CHANGE_APPLICATION_STATE));
        updateOfflineView();
    }

    private void updateOfflineView() {
        if (NowApplication.getState() == NowApplication.APP_STATE.ONLINE) {
            mOfflineView.setVisibility(View.GONE);
        } else {
            mOfflineView.setVisibility(View.VISIBLE);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.event_place_holder);
                if (fragment != null && fragment.isVisible()) {
                    fragmentManager.popBackStack();
                }
                return true;
            case R.id.action_about:

                MaterialDialog dialog = new MaterialDialog.Builder(this)
                        .title(getResources().getString(R.string.action_about))
                        .typeface(RobotoTypefaceManager.obtainTypeface(getApplicationContext(), RobotoTypefaceManager.Typeface.ROBOTO_REGULAR),
                                RobotoTypefaceManager.obtainTypeface(getApplicationContext(), RobotoTypefaceManager.Typeface.ROBOTO_REGULAR))
                        .disableDefaultFonts()
                        .customView(R.layout.about_layout, false)
                        .show();
                View view = dialog.getCustomView();
                ((TextView) view.findViewById(R.id.text_view)).setMovementMethod(LinkMovementMethod.getInstance());

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onEventItemSelect(EventItem eventItem) {
        if (mEventDetailFragment == null) {
            mEventDetailFragment = EventDetailFragment.getInstance(eventItem);
        } else {
            mEventDetailFragment.updateEventItem(eventItem);
        }
        if (!mEventDetailFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).addToBackStack(null).replace(R.id.event_place_holder, mEventDetailFragment, EventDetailFragment.TAG).commit();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event_collection, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NowApplication.setConnectionReceiverActive(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        NowApplication.setConnectionReceiverActive(false);
    }

}
