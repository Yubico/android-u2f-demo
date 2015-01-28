package com.yubico.u2fdemo.app.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.yubico.u2fdemo.app.R;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 8/14/13
 * Time: 10:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class DetailsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_details, container, false);

        ViewGroup details = (ViewGroup) view.findViewById(R.id.details_container);
        details.removeAllViews();
        for (String key : getArguments().getStringArrayList("keys")) {
            TextView text = new TextView(getActivity());
            String value = getArguments().getString(key);
            if (value.equals("--header--")) {
                text.setText(key);
                text.setTypeface(null, Typeface.BOLD);
                text.setPadding(0, 16, 0, 0);
            } else {
                text.setText(key.trim() + ": " + value.trim());
            }
            details.addView(text);
        }

        return view;
    }
}
