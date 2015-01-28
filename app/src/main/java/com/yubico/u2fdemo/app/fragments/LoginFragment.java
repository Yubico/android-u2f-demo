package com.yubico.u2fdemo.app.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.yubico.u2fdemo.app.OnLoginEnteredListener;
import com.yubico.u2fdemo.app.R;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 8/12/13
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoginFragment extends Fragment implements TextView.OnEditorActionListener, View.OnClickListener {
    private OnLoginEnteredListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        ((EditText) view.findViewById(R.id.password)).setOnEditorActionListener(this);
        view.findViewById(R.id.sign_button).setOnClickListener(this);

        view.findViewById(R.id.enroll_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText userText = (EditText) getActivity().findViewById(R.id.username);
                String username = userText.getText().toString().trim();
                EditText passText = (EditText) getActivity().findViewById(R.id.password);
                String password = passText.getText().toString().trim();

                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if(getActivity().getCurrentFocus() != null) {
                    inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getApplicationWindowToken(), 0);
                }

                if(username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.fill_out, Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d("login-fragment", "user/pass: " + username + "/" + password);
                listener.onLoginEntered(username, password, false);
            }
        });

        return view;
    }

    private void submit() {
        EditText userText = (EditText) getActivity().findViewById(R.id.username);
        String username = userText.getText().toString().trim();
        EditText passText = (EditText) getActivity().findViewById(R.id.password);
        String password = passText.getText().toString().trim();

        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if(getActivity().getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getApplicationWindowToken(), 0);
        }

        if(username.isEmpty() || password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.fill_out, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("login-fragment", "user/pass: " + username + "/" + password);
        listener.onLoginEntered(username, password, true);
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO && listener != null) {
            submit();
            return true;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        submit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnLoginEnteredListener) activity;
    }
}
