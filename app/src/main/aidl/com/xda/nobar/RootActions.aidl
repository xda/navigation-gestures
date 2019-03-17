package com.xda.nobar;

import java.lang.String;

interface RootActions {
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
