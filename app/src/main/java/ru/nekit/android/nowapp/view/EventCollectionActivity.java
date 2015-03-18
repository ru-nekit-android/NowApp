package ru.nekit.android.nowapp.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;
import ru.nekit.android.nowapp.modelView.IEventItemSelectListener;


public class EventCollectionActivity extends ActionBarActivity implements IEventItemSelectListener {

    private EventCollectionFragment mEventCollectionFragment;
    private EventItemDetailFragment mEventItemDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_collection);

        if (savedInstanceState == null) {
            mEventCollectionFragment = new EventCollectionFragment();
            mEventItemDetailFragment = new EventItemDetailFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.event_place_holder, mEventCollectionFragment, EventCollectionFragment.TAG).commit();

        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment fragment = fragmentManager.findFragmentByTag(EventItemDetailFragment.TAG);
                if (fragment != null && fragment.isVisible()) {
                    fragmentManager.popBackStack();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onEventItemSelect(EventItem eventItem) {
        mEventItemDetailFragment.setEventItem(eventItem);
        getSupportFragmentManager().beginTransaction().replace(R.id.event_place_holder, mEventItemDetailFragment, EventItemDetailFragment.TAG).addToBackStack(null).commit();

    }
}
