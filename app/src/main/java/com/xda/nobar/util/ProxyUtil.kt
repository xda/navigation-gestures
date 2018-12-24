package com.xda.nobar.util

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.graphics.Rect
import android.hardware.input.InputManager
import android.os.Bundle
import android.os.IBinder
import android.os.UserHandle
import android.view.Display
import android.view.InputEvent
import android.view.inputmethod.InputMethodManager
import com.xda.nobar.App
import com.xda.nobar.BuildConfig

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
            .invoke(null, prop) as String
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

fun SearchManager.launchAssist() {
    this::class.java.getMethod("launchAssist", String::class.java).invoke(this, null)
}

fun SearchManager.launchLegacyAssist() {
    this::class.java.getMethod("launchLegacyAssist", String::class.java, Int::class.java, Bundle::class.java)
            .invoke(this, null, -2, null)
}

fun InputManager.injectInputEvent(event: InputEvent, mode: Int) {
    this::class.java.getMethod("injectInputEvent", InputEvent::class.java, Int::class.java)
            .invoke(this, event, mode)
}

fun checkEMUI() = getSystemProperty("ro.build.version.emui") != null

val iPackageManager: Any
    @SuppressLint("PrivateApi")
    get() {
        val aThreadClass = Class.forName("android.app.ActivityThread")
        return aThreadClass.getMethod("getPackageManager")
                .invoke(null)
    }

fun grantPermission(permission: String): Boolean {
    val iPM = iPackageManager

    val grantRuntimePermission = iPM::class.java
            .getMethod("grantRuntimePermission", String::class.java,
                    String::class.java, UserHandle::class.java)

    val userCurrent =
            UserHandle::class.java.getConstructor(Int::class.java).newInstance(-2)

    return grantRuntimePermission
            .invoke(iPM, BuildConfig.APPLICATION_ID, permission, userCurrent) as Boolean
}

val Context.app: App
    get() = applicationContext as App
