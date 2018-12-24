package com.xda.nobar.root

import android.content.Context
import com.topjohnwu.superuser.Shell
import com.xda.nobar.RootActions
import eu.chainfire.librootjava.BuildConfig
import eu.chainfire.librootjava.RootIPCReceiver
import eu.chainfire.librootjava.RootJava
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RootWrapper(private val context: Context) {
    private val receiver = object : RootIPCReceiver<RootActions>(null, 0) {
        override fun onConnect(ipc: RootActions?) {
            actions = ipc
        }

        override fun onDisconnect(ipc: RootActions?) {
            actions = null
        }
    }
    private var isCreated = false

    var actions: RootActions? = null

    fun onCreate() {
        if (!isCreated) {
            isCreated = true

            val script =
                    RootJava.getLaunchScript(context, RootHandler::class.java, null,
                            null, null, BuildConfig.APPLICATION_ID + ":root")

            if (Shell.rootAccess()) {
                Shell.su(*script.toTypedArray())
                        .submit()
            }

            receiver.setContext(context)
        }
    }

    fun onDestroy() {
        receiver.release()
        GlobalScope.launch {
            RootJava.cleanupCache(context)
        }
        isCreated = false
    }
}