package com.xda.nobar.util

import android.annotation.SuppressLint
import android.graphics.Rect
import android.hardware.input.InputManager
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.View
import android.view.inputmethod.InputMethodManager

const val POLICY_CONTROL = "policy_control"

val InputMethodManager.inputMethodWindowVisibleHeight: Int
    get() {
        val method = this::class.java.getMethod("getInputMethodWindowVisibleHeight")
        return method.invoke(this) as Int
    }

val inputManager: InputManager
    get() =
        InputManager::class.java.getMethod("getInstance")
                .invoke(null) as InputManager

fun Display.getOverscanInsets(out: Rect) {
    val method = this::class.java.getMethod("getOverscanInsets", Rect::class.java)
    method.invoke(this, out)
}

@SuppressLint("PrivateApi")
fun getSystemProperty(prop: String): String? {
    return Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, prop) as String?
}

private val iStatusBarManager: Any
    @SuppressLint("PrivateApi")
    get() {
        val serviceMan = Class.forName("android.os.ServiceManager")
        val checkService = serviceMan.getMethod("checkService", String::class.java)
        val stub = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
        val asInterface = stub.getMethod("asInterface", IBinder::class.java)

        return asInterface.invoke(null, checkService.invoke(null, "statusbar"))
    }

fun expandNotificationsPanel() {
    val manager = iStatusBarManager
    manager::class.java.getMethod("expandNotificationsPanel")
            .invoke(manager)
}

fun expandSettingsPanel() {
    val manager = iStatusBarManager
    try {
        manager::class.java.getMethod("expandSettingsPanel", String::class.java)
                .invoke(manager, null)
    } catch (e: Exception) {
        manager::class.java.getMethod("expandSettingsPanel")
                .invoke(manager)
    }
}

fun checkEMUI() = !getSystemProperty("ro.build.version.emui").isNullOrEmpty()

fun killAllBackgroundProcesses() {
    val iAmStub = Class.forName("android.app.IActivityManager\$Stub")
    val serviceMan = Class.forName("android.os.ServiceManager")
    val checkService = serviceMan.getMethod("checkService", String::class.java)

    val asInterface = iAmStub.getMethod("asInterface", IBinder::class.java)
    val iAmInstance = asInterface.invoke(null, checkService.invoke(null, "activity"))

    val killAllBackgroundProcesses = iAmStub.getMethod("killAllBackgroundProcesses")
    killAllBackgroundProcesses.invoke(iAmInstance)
}

fun printAddedViews() {
    val wmg = Class.forName("android.view.WindowManagerGlobal")
    val wmgInstance = wmg.getMethod("getInstance").invoke(null)

    val viewsField = wmg.getDeclaredField("mViews")
    viewsField.isAccessible = true

    val views = viewsField.get(wmgInstance) as List<View>

    views.forEach {
        Log.e("NoBar", it::class.java.canonicalName)
    }
}
