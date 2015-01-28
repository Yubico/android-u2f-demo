package com.yubico.u2fdemo.app;

public interface OnLoginEnteredListener {
    void onLoginEntered(String username, String password, boolean sign);
}
