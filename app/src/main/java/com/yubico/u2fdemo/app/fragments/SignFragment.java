package com.yubico.u2fdemo.app.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.yubico.u2fdemo.app.MainActivity;
import com.yubico.u2fdemo.app.R;
import com.yubico.u2fdemo.app.model.APDUError;
import com.yubico.u2fdemo.app.model.U2F_V2;
import com.yubico.u2fdemo.app.util.HTTP;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 8/12/13
 * Time: 10:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class SignFragment extends Fragment implements MainActivity.OnNFCListener {
    private final Map<String, String> details;
    private final String username;
    private final String password;
    private NfcAdapter adapter;
    private String challengeJson;

    public SignFragment(Map<String, String> details, String username, String password) {
        this.username = username;
        this.password = password;
        this.details = details;
        this.details.put("Login Data", "--header--");
        this.details.put("username", username);
        this.details.put("password", password);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = NfcAdapter.getDefaultAdapter(getActivity());

        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("username", username);
        parameters.put("password", password);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String response = HTTP.post(new URL(MainActivity.SIGN_URL), parameters);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                challengeJson = response;
                                onChallengeReceived();
                            } catch (Exception e) {
                                getView().findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                                ((TextView) getView().findViewById(R.id.status_text)).setText(e.getMessage());
                            }
                        }
                    });
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) getView().findViewById(R.id.status_text)).setText(R.string.wrong_creds);
                            getView().findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    public void onPause() {
        super.onPause();
        adapter.disableForegroundDispatch(getActivity());
    }

    public void onResume() {
        super.onResume();

        Intent intent = getActivity().getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tagIntent = PendingIntent.getActivity(getActivity(), 0, intent, 0);
        IntentFilter iso = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        adapter.enableForegroundDispatch(getActivity(), tagIntent, new IntentFilter[]{iso},
                new String[][]{new String[]{IsoDep.class.getName()}});
    }

    private void onChallengeReceived() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (adapter == null) {
            Toast.makeText(getActivity(), R.string.no_nfc, Toast.LENGTH_LONG).show();
        } else if (adapter.isEnabled()) {
            Intent intent = getActivity().getIntent();
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent tagIntent = PendingIntent.getActivity(getActivity(), 0, intent, 0);
            IntentFilter iso = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

            ((MainActivity) getActivity()).setOnNFCListener(SignFragment.this);
            adapter.enableForegroundDispatch(getActivity(), tagIntent, new IntentFilter[]{iso},
                    new String[][]{new String[]{IsoDep.class.getName()}});

            getView().findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            ((TextView) getView().findViewById(R.id.status_text)).setText(R.string.swipe);
        } else {
            Toast.makeText(getActivity(), R.string.nfc_disabled, Toast.LENGTH_LONG).show();
        }
    }

    private void handleResult(JSONObject data) throws JSONException {
        byte touch = (byte) data.getString("touch").charAt(0);
        int counter = data.getInt("counter");
        details.put("Authentication Parameters", "--header--");
        details.put("counter", counter + "");
        details.put("touch", "0x" + new String(Hex.encodeHex(new byte[]{touch})));
    }

    private void runOnUiThread(Runnable runnable) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(runnable);
        } else {
            Log.d("challenge-fragment", "Activity is null!");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign, container, false);
    }

    @Override
    public void onNFC(IsoDep isoTag) {
        try {
            U2F_V2 u2f = new U2F_V2(isoTag);

            final URL verifyUrl = new URL(MainActivity.VERIFY_URL);
            final Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("username", username);
            parameters.put("password", password);
            parameters.put("data", u2f.sign(challengeJson, MainActivity.ORIGIN));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String response = HTTP.post(verifyUrl, parameters);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject data = (JSONObject) new JSONTokener(response).nextValue();
                                    handleResult(data);
                                    ((TextView) getView().findViewById(R.id.status_text)).setText(R.string.success);
                                    getView().findViewById(R.id.details).setVisibility(View.VISIBLE);
                                } catch (JSONException e) {
                                    Log.e("challenge-fragment", "Invalid response", e);
                                    ((TextView) getView().findViewById(R.id.status_text)).setText(R.string.failure);
                                }
                                getView().findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                            }
                        });
                    } catch (IOException e) {
                        Log.e("challenge-fragment", "Failed HTTP", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) getView().findViewById(R.id.status_text)).setText(R.string.failure);
                            }
                        });
                    }
                }
            }).start();
        } catch (APDUError apduError) {
            Log.e("challenge-fragment", apduError.getMessage());
            Toast.makeText(getActivity(), R.string.sign_failed, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e("challenge-fragment", "JSONException", e);
            Toast.makeText(getActivity(), R.string.sign_failed, Toast.LENGTH_LONG).show();
        }
    }
}
