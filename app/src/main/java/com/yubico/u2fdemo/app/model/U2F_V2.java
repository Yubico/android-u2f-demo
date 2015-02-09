package com.yubico.u2fdemo.app.model;

import android.nfc.tech.IsoDep;
import android.util.Base64;
import android.util.Log;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by dain on 2/28/14.
 */
public class U2F_V2 {
    private static final byte[] SELECT_COMMAND = {0x00, (byte) 0xa4, 0x04, 0x00, 0x08, (byte)0xa0, 0x00, 0x00, 0x06, 0x47, 0x2f, 0x00, 0x01};
    private static final byte[] SELECT_COMMAND_YUBICO = {0x00, (byte) 0xa4, 0x04, 0x00, 0x07, (byte) 0xa0, 0x00, 0x00, 0x05, 0x27, 0x10, 0x02};
    private static final byte[] GET_RESPONSE_COMMAND = {0x00, (byte) 0xc0, 0x00, 0x00, (byte) 0xff};
    private static final byte[] GET_VERSION_COMMAND = {0x00, (byte) 0x03, 0x00, 0x00, (byte) 0xff};

    private final IsoDep tag;

    public U2F_V2(IsoDep tag) throws IOException, APDUError {
        this.tag = tag;
        tag.setTimeout(5000);
        tag.connect();
        try {
            send(SELECT_COMMAND);
        } catch (APDUError e) {
            if(e.getCode() == 0x6a82) {
                send(SELECT_COMMAND_YUBICO);
            } else {
                throw e;
            }
        }
    }

    public String getVersion() throws IOException, APDUError {
        return new String(send(GET_VERSION_COMMAND), Charset.forName("ASCII"));
    }

    public String enroll(String jsonRequest, String origin) throws JSONException, IOException, APDUError {
        JSONObject request = (JSONObject) new JSONTokener(jsonRequest).nextValue();
        if (!request.getString("version").equals("U2F_V2")) {
            throw new RuntimeException("Unsupported U2F_V2 version!");
        }

        byte[] appParam = DigestUtils.sha256(request.getString("appId"));

        JSONObject clientData = new JSONObject();
        clientData.put("typ", "navigator.id.finishEnrollment");
        clientData.put("challenge", request.getString("challenge"));
        clientData.put("origin", origin);
        String clientDataString = clientData.toString();
        byte[] clientParam = DigestUtils.sha256(clientDataString);

        byte[] apdu = new byte[5 + 32 + 32 + 1];
        apdu[1] = 0x01; //ins = ENROLL
        apdu[2] = 0x03; //p1
        apdu[4] = 64; //length
        apdu[69] = (byte) 0xff; //256 byte response
        System.arraycopy(clientParam, 0, apdu, 5, 32);
        System.arraycopy(appParam, 0, apdu, 5 + 32, 32);

        byte[] resp = send(apdu);

        JSONObject response = new JSONObject();
        response.put("registrationData", Base64.encodeToString(resp, 0, resp.length, Base64.URL_SAFE));
        response.put("clientData", Base64.encodeToString(clientDataString.getBytes(Charset.forName("ASCII")), Base64.URL_SAFE));

        return response.toString();
    }

    public String sign(String jsonRequest, String origin) throws JSONException, IOException, APDUError {
        JSONObject request = (JSONObject) new JSONTokener(jsonRequest).nextValue();
        if (!request.getString("version").equals("U2F_V2")) {
            throw new RuntimeException("Unsupported U2F_V2 version!");
        }

        byte[] appParam = DigestUtils.sha256(request.getString("appId"));
        byte[] keyHandle = Base64.decode(request.getString("keyHandle"), Base64.URL_SAFE);

        JSONObject clientData = new JSONObject();
        clientData.put("typ", "navigator.id.getAssertion");
        clientData.put("challenge", request.getString("challenge"));
        clientData.put("origin", origin);
        String clientDataString = clientData.toString();
        byte[] clientParam = DigestUtils.sha256(clientDataString);


        byte[] apdu = new byte[5 + 32 + 32 + 1 + keyHandle.length + 1];
        apdu[1] = 0x02; //ins = SIGN
        apdu[2] = 0x03; //p1
        apdu[4] = (byte) (64 + 1 + keyHandle.length); //length
        apdu[apdu.length - 1] = (byte) 0xff;
        System.arraycopy(clientParam, 0, apdu, 5, 32);
        System.arraycopy(appParam, 0, apdu, 5 + 32, 32);
        apdu[5 + 64] = (byte) keyHandle.length;
        System.arraycopy(keyHandle, 0, apdu, 5 + 64 + 1, keyHandle.length);

        byte[] resp = send(apdu);

        JSONObject response = new JSONObject();
        response.put("signatureData", Base64.encodeToString(resp, 0, resp.length, Base64.URL_SAFE));
        response.put("clientData", Base64.encodeToString(clientDataString.getBytes(Charset.forName("ASCII")), Base64.URL_SAFE));
        response.put("challenge", request.getString("challenge"));
        response.put("appId", request.getString("appId"));

        return response.toString();
    }

    private byte[] send(byte[] apdu) throws IOException, APDUError {
        byte[] cmd = apdu;
        int status = 0x6100;
        byte[] data = new byte[0];

        while ((status & 0xff00) == 0x6100) {
            byte[] resp = tag.transceive(cmd);
            Log.d("REQ ", new String(Hex.encodeHex(cmd)));
            Log.d("RESP", new String(Hex.encodeHex(resp)));
            status = ((0xff & resp[resp.length - 2]) << 8) | (0xff & resp[resp.length - 1]);
            data = concat(data, resp, resp.length - 2);
            cmd = GET_RESPONSE_COMMAND;
        }

        if (status != 0x9000) {
            throw new APDUError(status);
        }

        return data;
    }

    private static byte[] concat(byte[] a, byte[] b, int length) {
        byte[] res = new byte[a.length + length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, length);
        return res;
    }
}
