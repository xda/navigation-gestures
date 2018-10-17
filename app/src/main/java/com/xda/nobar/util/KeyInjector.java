package com.xda.nobar.util;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.KeyEvent;

import java.io.InputStreamReader;
import java.util.Scanner;

public class KeyInjector {
    private static InputManager inputManager = InputManager.getInstance();

    public static void main(final String[] args) {
        Scanner scanner = new Scanner(new InputStreamReader(System.in));

        while(true) {
            try {
                String input = scanner.nextLine();

                if (input == null) continue;

                if (input.equals("exit")) break;

                String[] stuff = input.split(" ");
                handleArgs(stuff);
            } catch (Exception ignored) {}
        }
    }

    private static void handleArgs(final String[] args) {
        if (args.length > 0) {
            final String code = args[0];
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

//        injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, repeat ? 1 : 0, 0,
//                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
//                KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD));
//
//        injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, 0,
//                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
//                KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD));

        try {
            if (longPress) {

            } else {
                injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0));
                injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0));
                if (repeat) {
                    Thread.sleep(50);
                    injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0));
                    injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0));
                }
            }
        } catch (Exception ignored) {}
    }

    private static void injectKeyEvent(KeyEvent event) {
        inputManager.injectInputEvent(event,
                InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }
}
