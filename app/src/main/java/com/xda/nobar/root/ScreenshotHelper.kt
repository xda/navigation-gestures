package com.xda.nobar.root

import android.app.ActivityManager
import android.app.ActivityThread
import android.app.IServiceConnection
import android.app.LoadedApk
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log

class ScreenshotHelper(private val context: Context, private val activityThread: ActivityThread, private val handler: Handler) {
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

//    private fun bindService(intent: Intent, conn: ServiceConnection, flags: Int, userHandle: UserHandle): Boolean {
//        var mutableFlags = flags
//
//        // Keep this in sync with DevicePolicyManager.bindDeviceAdminServiceAsUser.
//        val sd: IServiceConnection
//
//        val mPackageInfo = Class.forName("android.app.ContextImpl")
//                .getDeclaredField("mPackageInfo")
//                .apply { isAccessible = true }
//                .get(context) as LoadedApk?
//
//        val activityToken = Class.forName("android.app.ContextImpl")
//                .getDeclaredField("mActivityToken")
//                .apply { isAccessible = true }
//                .get(context) as IBinder?
//
//        if (mPackageInfo != null) {
//            sd = (mPackageInfo).getServiceDispatcher(conn, context, handler, flags)
//        } else {
//            throw RuntimeException("Not supported in system context")
//        }
////        validateServiceIntent(service)
//        try {
//            if (activityToken == null && mutableFlags and Context.BIND_AUTO_CREATE == 0
//                    && mPackageInfo.applicationInfo.targetSdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                mutableFlags = mutableFlags or Context.BIND_WAIVE_PRIORITY
//            }
//            intent.prepareToLeaveProcess(context)
//            val res = ActivityManager.getService().bindService(
//                    null, activityToken, intent,
//                    intent.resolveTypeIfNeeded(context.contentResolver),
//                    sd, mutableFlags, context.opPackageName, context.user.identifier)
//            if (res < 0) {
//                throw SecurityException(
//                        "Not allowed to bind to service $intent")
//            }
//            return res != 0
//        } catch (e: RemoteException) {
//            throw e.rethrowFromSystemServer()
//        }
//    }

    companion object {
        private const val TAG = "ScreenshotHelper"

        const val SYSUI_PACKAGE = "com.android.systemui"
        const val SYSUI_SCREENSHOT_SERVICE = "com.android.systemui.screenshot.TakeScreenshotService"
        const val SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver"

        // Time until we give up on the screenshot & show an error instead.
        private const val SCREENSHOT_TIMEOUT_MS = 10000
    }

}
