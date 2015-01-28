package com.yubico.u2fdemo.app;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;
import com.yubico.u2fdemo.app.fragments.DetailsFragment;
import com.yubico.u2fdemo.app.fragments.EnrollFragment;
import com.yubico.u2fdemo.app.fragments.LoginFragment;
import com.yubico.u2fdemo.app.fragments.SignFragment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


public class MainActivity extends FragmentActivity implements OnLoginEnteredListener {
    public final static String ORIGIN = "https://demo.yubico.com";
    private final static String BASE = ORIGIN + "/wsapi/u2f/";
    public final static String ENROLL_URL = BASE + "enroll";
    public final static String BIND_URL = BASE + "bind";
    public final static String SIGN_URL = BASE + "sign";
    public final static String VERIFY_URL = BASE + "verify";

    private final Map<String,String> details = new LinkedHashMap<String, String>();
    private OnNFCListener nfc_listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LoginFragment fragment = new LoginFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    public void onLoginEntered(String username, String password, boolean sign) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        details.clear();
        Fragment fragment = sign ? new SignFragment(details, username, password) : new EnrollFragment(details, username, password);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        Log.d("login-activity", "transaction committed: " + fragmentTransaction);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tag != null && nfc_listener != null) {
            nfc_listener.onNFC(IsoDep.get(tag));
        }
    }

    public void restart(View view) {
        details.clear();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LoginFragment fragment = new LoginFragment();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public void details(View view) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);
        DetailsFragment fragment = new DetailsFragment();
        Bundle bundle = new Bundle();
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(details.keySet());
        bundle.putStringArrayList("keys", keys);
        for(Map.Entry<String, String> entry : details.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        fragment.setArguments(bundle);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public void setOnNFCListener(OnNFCListener listener) {
        nfc_listener = listener;
    }

    public interface OnNFCListener {
        void onNFC(IsoDep tag);
    }
}
