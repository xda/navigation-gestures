package com.xda.nobar.util;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.xda.nobar.App;

import java.io.InputStreamReader;
import java.util.Scanner;

public class KeyInjector {
    public static void main(final String[] args) {
        Scanner scanner = new Scanner(new InputStreamReader(System.in));

        while(true) {
            String input = scanner.nextLine();

            if (input != null && input.equals("exit")) break;

            if (input != null) {
                String[] stuff = input.split(" ");
                handleArgs(stuff);
            }
        }
    }

    private static void handleArgs(final String[] args) {
        if (args.length > 0) {
            String code = args[0];
            String flag = null;

            if (args.length > 1) {
                flag = args[1];
            }

            sendKeyEvent(Integer.valueOf(code), flag);
        }
    }

    private static void sendKeyEvent(int code, String flag) {
        boolean longPress = flag != null && flag.contains("longpress");
        boolean repeat = flag != null && flag.contains("repeat");

        long now = SystemClock.uptimeMillis();

        injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD));

        if (longPress) injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 1, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD));

        injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD));

        if (repeat) {
            injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD));

            injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD));
        }
    }

    private static void injectKeyEvent(KeyEvent event) {
        App.Companion.getINPUT_MANAGER().injectInputEvent(event,
                InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }
}
