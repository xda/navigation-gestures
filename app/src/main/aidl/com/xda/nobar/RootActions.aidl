package com.xda.nobar;

import java.lang.String;

interface RootActions {
    void sendKeyEvent(int code);
    void sendLongKeyEvent(int code);
    void sendDoubleKeyEvent(int code);
    void lockScreen();
    void screenshot();
    void killCurrentApp();
}
