package com.xda.nobar.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Display
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * Proxy class for android.view.IWindowManager
 */
@SuppressLint("PrivateApi")
object IWindowManager {
    const val NAV_BAR_LEFT = 1 shl 0
    const val NAV_BAR_RIGHT = 1 shl 1
    const val NAV_BAR_BOTTOM = 1 shl 2

    private val iWindowManagerClass: Class<*> = Class.forName("android.view.IWindowManager")
    private val iWindowManager: Any = run {
        val stubClass = Class.forName("android.view.IWindowManager\$Stub")
        val serviceManagerClass = Class.forName("android.os.ServiceManager")

        val binder = serviceManagerClass.getMethod("checkService", String::class.java).invoke(null, Context.WINDOW_SERVICE)
        stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
    }

    /**
     * Set overscan from given values
     * @param left overscan left
     * @param top overscan top
     * @param right overscan right
     * @param bottom overscan bottom
     */
    fun setOverscan(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        return try {
            iWindowManagerClass
                    .getMethod("setOverscan", Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                    .invoke(iWindowManager, Display.DEFAULT_DISPLAY, left, top, right, bottom)
            canRunCommands()
        } catch (e: Throwable) {
            GlobalScope.launch {
                try {
                    Shell.SH.run("wm overscan $left,$top,$right,$bottom")
                } catch (e: Exception) {}
            }

            true
        }
    }

    /**
     * Check if the device is able to execute WindowManager commands
     * @return true if the above condition is met
     */
    fun canRunCommands(): Boolean {
        return try {
            Class.forName("android.view.IWindowManager")
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun getStableInsetsForDefaultDisplay(rect: Rect): Boolean {
        return try {
            iWindowManagerClass
                    .getMethod("getStableInsets", Int::class.java, Rect::class.java)
                    .invoke(iWindowManager, Display.DEFAULT_DISPLAY, rect)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun hasNavigationBar(): Boolean {
        return try {
            iWindowManagerClass
                    .getMethod("hasNavigationBar")
                    .invoke(iWindowManager) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    fun getNavBarPosition(): Int {
        return try {
            iWindowManagerClass
                    .getMethod("getNavBarPosition")
                    .invoke(iWindowManager) as Int
        } catch (e: Exception) {
            Integer.MIN_VALUE
        }
    }
}