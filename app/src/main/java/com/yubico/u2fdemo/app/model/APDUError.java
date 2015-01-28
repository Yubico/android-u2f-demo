package com.yubico.u2fdemo.app.model;

/**
 * Created by dain on 2/28/14.
 */
public class APDUError extends Exception {
    private final int code;

    public APDUError(int code) {
        super(String.format("APDU status: %04x", code));
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
