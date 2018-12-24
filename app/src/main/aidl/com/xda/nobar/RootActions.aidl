package com.xda.nobar;

import java.lang.String;

interface RootActions {
    boolean grantPermission(in String permission);

    void sendKeyEvent(int code);
    void sendLongKeyEvent(int code);
    void sendDoubleKeyEvent(int code);
    void lockScreen();
    void goHome();
    void openRecents();
    void goBack();
    void switchApps();
    void splitScreen();
    void openPowerMenu();
}
