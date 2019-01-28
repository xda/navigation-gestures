package com.xda.nobar.root;

import android.Manifest;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.xda.nobar.BuildConfig;
import com.xda.nobar.RootActions;
import com.xda.nobar.util.ProxyUtilKt;

import eu.chainfire.librootjava.RootIPC;
import eu.chainfire.librootjava.RootJava;

public class RootHandler {
    private static Handler handler;

    public static void main(String[] args) {
        Looper.prepare();
        handler = new Handler();

        RootJava.restoreOriginalLdLibraryPath();

        IBinder actions = new RootActionsImpl();

        try {
            new RootIPC(BuildConfig.APPLICATION_ID, actions, 200, 30 * 1000, true);
        } catch (RootIPC.TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static class RootActionsImpl extends RootActions.Stub {
        @Override
        public boolean grantPermission(String permission) {
            try {
                Runtime.getRuntime().exec("pm grant " + BuildConfig.APPLICATION_ID + " " + Manifest.permission.WRITE_SECURE_SETTINGS);
            } catch (Exception ignored) {}
            return true;
        }

        @Override
        public void sendKeyEvent(int code) {
            long now = SystemClock.uptimeMillis();

            injectKeyEvent(createKeyEvent(
                    now,
                    KeyEvent.ACTION_DOWN,
                    code,
                    0, 0
            ));
            injectKeyEvent(createKeyEvent(
                    now,
                    KeyEvent.ACTION_UP,
                    code,
                    0, 0
            ));
        }

        @Override
        public void sendDoubleKeyEvent(int code) {
            sendKeyEvent(code);

            handler.postDelayed(() -> sendKeyEvent(code), 10);
        }

        @Override
        public void sendLongKeyEvent(int code) {
            long now = SystemClock.uptimeMillis();

            KeyEvent event = createKeyEvent(now, KeyEvent.ACTION_DOWN, code, 0, KeyEvent.FLAG_LONG_PRESS);
            injectKeyEvent(event);

            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_UP, code, 0, 0));
        }

        @Override
        public void lockScreen() {
            sendKeyEvent(KeyEvent.KEYCODE_POWER);
        }

        @Override
        public void goHome() {
            sendKeyEvent(KeyEvent.KEYCODE_HOME);
        }

        @Override
        public void openRecents() {
            sendKeyEvent(KeyEvent.KEYCODE_APP_SWITCH);
        }

        @Override
        public void goBack() {
            sendKeyEvent(KeyEvent.KEYCODE_BACK);
        }

        @Override
        public void switchApps() {
            sendDoubleKeyEvent(KeyEvent.KEYCODE_APP_SWITCH);
        }

        @Override
        public void splitScreen() {
            sendLongKeyEvent(KeyEvent.KEYCODE_APP_SWITCH);
        }

        @Override
        public void openPowerMenu() {
            sendLongKeyEvent(KeyEvent.KEYCODE_POWER);
        }

        private void injectKeyEvent(KeyEvent event) {
            ProxyUtilKt.injectInputEvent(
                    ProxyUtilKt.getInputManager(),
                    event,
                    0);
        }

        private KeyEvent createKeyEvent(long now, int action, int code, int repeat, int flags) {
            return new KeyEvent(
                    now, now,
                    action, code,
                    repeat, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0, flags,
                    InputDevice.SOURCE_KEYBOARD
            );
        }
    }
}
