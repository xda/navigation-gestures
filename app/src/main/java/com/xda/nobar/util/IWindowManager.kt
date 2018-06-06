package com.xda.nobar.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.IBinder
import android.view.Display


/**
 * Proxy class for android.view.IWindowManager
 */
@SuppressLint("PrivateApi")
object IWindowManager {
    private var iWindowManagerClass: Class<*>? = null
    private var iWindowManager: Any? = null

    init {
//        if (canRunCommands()) {
//
//        }
        iWindowManagerClass = Class.forName("android.view.IWindowManager")
        val stubClass = Class.forName("android.view.IWindowManager\$Stub")
        val serviceManagerClass = Class.forName("android.os.ServiceManager")

        val binder = serviceManagerClass.getMethod("checkService", String::class.java).invoke(null, Context.WINDOW_SERVICE)
        iWindowManager = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
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
                    ?.getMethod("setOverscan", Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                    ?.invoke(iWindowManager, Display.DEFAULT_DISPLAY, left, top, right, bottom)
            canRunCommands()
        } catch (e: Throwable) {
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
                    ?.getMethod("getStableInsets", Int::class.java, Rect::class.java)
                    ?.invoke(iWindowManager, Display.DEFAULT_DISPLAY, rect)
            true
        } catch (e: Exception) {
            false
        }
    }
}