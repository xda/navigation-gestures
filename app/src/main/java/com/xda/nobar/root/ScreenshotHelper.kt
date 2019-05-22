package com.xda.nobar.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log

class ScreenshotHelper(private val context: Context) {
    private val mScreenshotLock = Any()
    private var mScreenshotConnection: ServiceConnection? = null

    /**
     * Request a screenshot be taken.
     *
     * @param screenshotType The type of screenshot, for example either
     * [android.view.WindowManager.TAKE_SCREENSHOT_FULLSCREEN]
     * or [android.view.WindowManager.TAKE_SCREENSHOT_SELECTED_REGION]
     * @param hasStatus `true` if the status bar is currently showing. `false` if not.
     * @param hasNav `true` if the navigation bar is currently showing. `false` if not.
     * @param handler A handler used in case the screenshot times out
     */
    fun takeScreenshot(screenshotType: Int, hasStatus: Boolean,
                       hasNav: Boolean, handler: Handler) {
        synchronized(mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return
            }
            val serviceComponent = ComponentName(SYSUI_PACKAGE,
                    SYSUI_SCREENSHOT_SERVICE)
            val serviceIntent = Intent()

            val mScreenshotTimeout = Runnable {
                synchronized(mScreenshotLock) {
                    if (mScreenshotConnection != null) {
                        context.unbindService(mScreenshotConnection)
                        mScreenshotConnection = null
                        notifyScreenshotError()
                    }
                }
            }

            serviceIntent.component = serviceComponent
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    synchronized(mScreenshotLock) {
                        if (mScreenshotConnection !== this) {
                            return
                        }
                        val messenger = Messenger(service)
                        val msg = Message.obtain(null, screenshotType)
                        val myConn = this
                        val h = object : Handler(handler.looper) {
                            override fun handleMessage(msg: Message) {
                                synchronized(mScreenshotLock) {
                                    if (mScreenshotConnection === myConn) {
                                        context.unbindService(mScreenshotConnection)
                                        mScreenshotConnection = null
                                        handler.removeCallbacks(mScreenshotTimeout)
                                    }
                                }
                            }
                        }
                        msg.replyTo = Messenger(h)
                        msg.arg1 = if (hasStatus) 1 else 0
                        msg.arg2 = if (hasNav) 1 else 0
                        try {
                            messenger.send(msg)
                        } catch (e: RemoteException) {
                            Log.e(TAG, "Couldn't take screenshot: $e")
                        }

                    }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    synchronized(mScreenshotLock) {
                        if (mScreenshotConnection != null) {
                            context.unbindService(mScreenshotConnection)
                            mScreenshotConnection = null
                            handler.removeCallbacks(mScreenshotTimeout)
                            notifyScreenshotError()
                        }
                    }
                }
            }
            if (context.bindServiceAsUser(serviceIntent, conn,
                            Context.BIND_AUTO_CREATE or Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE,
                            UserHandle.CURRENT)) {
                mScreenshotConnection = conn
                handler.postDelayed(mScreenshotTimeout, SCREENSHOT_TIMEOUT_MS.toLong())
            }
        }
    }

    /**
     * Notifies the screenshot service to show an error.
     */
    private fun notifyScreenshotError() {
        // If the service process is killed, then ask it to clean up after itself
        val errorComponent = ComponentName(SYSUI_PACKAGE,
                SYSUI_SCREENSHOT_ERROR_RECEIVER)
        // Broadcast needs to have a valid action.  We'll just pick
        // a generic one, since the receiver here doesn't care.
        val errorIntent = Intent(Intent.ACTION_USER_PRESENT)
        errorIntent.component = errorComponent
        errorIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT or Intent.FLAG_RECEIVER_FOREGROUND)
        context.sendBroadcastAsUser(errorIntent, UserHandle.CURRENT)
    }

    companion object {
        private val TAG = "ScreenshotHelper"

        private val SYSUI_PACKAGE = "com.android.systemui"
        private val SYSUI_SCREENSHOT_SERVICE = "com.android.systemui.screenshot.TakeScreenshotService"
        private const val SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver"

        // Time until we give up on the screenshot & show an error instead.
        private const val SCREENSHOT_TIMEOUT_MS = 10000
    }

}
