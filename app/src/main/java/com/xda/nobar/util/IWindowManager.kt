package com.xda.nobar.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Display
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.launch


/**
 * Proxy class for android.view.IWindowManager
 */
@SuppressLint("PrivateApi")
object IWindowManager {
    const val NAV_BAR_LEFT = 1 shl 0
    const val NAV_BAR_RIGHT = 1 shl 1
    const val NAV_BAR_BOTTOM = 1 shl 2

    var leftOverscan = 0
    var topOverscan = 0
    var rightOverscan = 0
    var bottomOverscan = 0

    private val queuedOverscanActions = ArrayList<OverscanInfo>()

    private val iWindowManagerClass: Class<*> = Class.forName("android.view.IWindowManager")
    private val iWindowManager: Any = run {
        val stubClass = Class.forName("android.view.IWindowManager\$Stub")
        val serviceManagerClass = Class.forName("android.os.ServiceManager")

        val binder = serviceManagerClass.getMethod("checkService", String::class.java).invoke(null, Context.WINDOW_SERVICE)
        stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
    }

    fun setOverscanAsync(left: Int, top: Int, right: Int, bottom: Int, listener: ((Boolean) -> Unit)? = null) {
        logicScope.launch {
            synchronized(queuedOverscanActions) {
                val ret = setOverscan(
                        left,
                        top,
                        right,
                        bottom
                )

                mainScope.launch {
                    listener?.invoke(ret)
                }
            }
        }
    }

    /**
     * Set overscan from given values
     * @param left overscan left
     * @param top overscan top
     * @param right overscan right
     * @param bottom overscan bottom
     */
    fun setOverscan(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        return if (leftOverscan != left
                || topOverscan != top
                || rightOverscan != right
                || bottomOverscan != bottom) {
            leftOverscan = left
            topOverscan = top
            rightOverscan = right
            bottomOverscan = bottom

            try {
                iWindowManagerClass
                        .getMethod("setOverscan", Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                        .invoke(iWindowManager, Display.DEFAULT_DISPLAY, left, top, right, bottom)
                canRunCommands()
            } catch (e: Throwable) {
                val res = Shell.run("sh", arrayOf("wm overscan $left,$top,$right,$bottom"), null, true)
                res.joinToString("").isBlank()
            }
        } else {
            false
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
            try {
                iWindowManagerClass
                        .getMethod("getNavBarPosition", Int::class.java)
                        .invoke(iWindowManager, Display.DEFAULT_DISPLAY) as Int
            } catch (e: Exception) {
                Integer.MIN_VALUE
            }
        }
    }

    private class OverscanInfo(
            val left: Int,
            val top: Int,
            val right: Int,
            val bottom: Int,
            val listener: ((Boolean) -> Unit)?
    )
}