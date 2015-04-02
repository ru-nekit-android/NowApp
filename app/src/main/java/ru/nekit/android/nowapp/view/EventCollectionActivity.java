package ru.nekit.android.nowapp.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemPosterSelectListener;
import ru.nekit.android.nowapp.modelView.listeners.IEventItemSelectListener;


public class EventCollectionActivity extends ActionBarActivity implements IEventItemSelectListener, IEventItemPosterSelectListener {

    private EventCollectionFragment mEventCollectionFragment;
    private EventDetailFragment mEventDetailFragment;
    private EventPosterViewFragment mEventPosterViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_collection);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_action_previous_item);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            mEventCollectionFragment = new EventCollectionFragment();
            mEventDetailFragment = new EventDetailFragment();
            mEventPosterViewFragment = new EventPosterViewFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.event_place_holder, mEventCollectionFragment, EventCollectionFragment.TAG).commit();
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
        mEventDetailFragment.setEventItem(eventItem);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).replace(R.id.event_place_holder, mEventDetailFragment, EventDetailFragment.TAG).addToBackStack(null).commit();

    }

    @Override
    public void onEventItemPosterSelect(String posterUrl) {
        mEventPosterViewFragment.setEventPosterUrl(posterUrl);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right).replace(R.id.event_place_holder, mEventPosterViewFragment, EventPosterViewFragment.TAG).addToBackStack(null).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event_collection, menu);
        return true;
    }
}
