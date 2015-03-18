package ru.nekit.android.nowapp.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.model.EventItem;

public class EventItemDetailFragment extends Fragment {

    public static final String TAG = "ru.nekit.android.event_item_detail_fragment";

    private static final String ARG_EVENT_ITEM = "ru.nekit.android.event_item";

    public EventItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arg = getArguments();
        EventItem eventItem = arg.getParcelable(ARG_EVENT_ITEM);
        View view = inflater.inflate(R.layout.fragment_event_item_detail, container, false);
        TextView textView = (TextView) view.findViewById(R.id.fake_text_view);
        textView.setText(String.format("Event item:%s", eventItem.name));
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setEventItem(EventItem eventItem) {
        Bundle arg = new Bundle();
        arg.putParcelable(ARG_EVENT_ITEM, eventItem);
        setArguments(arg);
    }
}
