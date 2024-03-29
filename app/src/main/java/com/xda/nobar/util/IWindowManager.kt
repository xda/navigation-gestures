package com.xda.nobar.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Display
import android.view.IRotationWatcher
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

    val hasOverscan: Boolean
        get() = leftOverscan != 0 || topOverscan != 0 || rightOverscan != 0 || bottomOverscan != 0

    private val queuedOverscanActions = Any()

    private val iWindowManagerClass: Class<*> = Class.forName("android.view.IWindowManager")
    private val iWindowManager: Any = run {
        val stubClass = Class.forName("android.view.IWindowManager\$Stub")
        val serviceManagerClass = Class.forName("android.os.ServiceManager")

        val binder = serviceManagerClass.getMethod("checkService", String::class.java).invoke(null, Context.WINDOW_SERVICE)
        stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
    }

    fun setOverscanAsync(left: Int, top: Int, right: Int, bottom: Int, listener: ((Boolean) -> Unit)? = null) = mainScope.launch {
        val ret = setOverscanAsyncInternal(left, top, right, bottom)

        listener?.invoke(ret)
    }

    private suspend fun setOverscanAsyncInternal(left: Int, top: Int, right: Int, bottom: Int) = withContext(Dispatchers.IO) {
        synchronized(queuedOverscanActions) {
            setOverscan(
                    left,
                    top,
                    right,
                    bottom
            )
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
        return try {
            iWindowManagerClass
                    .getMethod("setOverscan", Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                    .invoke(iWindowManager, Display.DEFAULT_DISPLAY, left, top, right, bottom)

            leftOverscan = left
            topOverscan = top
            rightOverscan = right
            bottomOverscan = bottom

            true
        } catch (e: Throwable) {
            val res = Shell.run("sh", arrayOf("wm overscan $left,$top,$right,$bottom"), null, true)
            res == null || res.joinToString("").isBlank()
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

    fun watchRotation(watcher: IRotationWatcher, displayId: Int): Int {
        return try {
            iWindowManagerClass
                    .run {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            getMethod("watchRotation", IRotationWatcher::class.java)
                                    .invoke(iWindowManager, watcher) as Int
                        } else {
                            getMethod("watchRotation", IRotationWatcher::class.java, Int::class.java)
                                    .invoke(iWindowManager, watcher, displayId) as Int
                        }
                    }
        } catch (e: Exception) {
            0
        }
    }

    fun removeRotationWatcher(watcher: IRotationWatcher) {
        try {
            iWindowManagerClass
                    .getMethod("removeRotationWatcher", IRotationWatcher::class.java)
                    .invoke(iWindowManager, watcher)
        } catch (e: Exception) {
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
}