package com.xda.nobar.root

import android.content.Context
import com.xda.nobar.RootActions
import com.xda.nobar.util.isSuAsync
import com.xda.nobar.util.logicScope
import eu.chainfire.librootjava.BuildConfig
import eu.chainfire.librootjava.RootIPCReceiver
import eu.chainfire.librootjava.RootJava
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.launch

class RootWrapper(private val context: Context) {
    private val receiver = object : RootIPCReceiver<RootActions>(context, 200, RootActions::class.java) {
        override fun onConnect(ipc: RootActions) {
            synchronized(queuedActions) {
                actions = ipc

                queuedActions.forEach { it.invoke(ipc) }
            }
        }

        override fun onDisconnect(ipc: RootActions?) {
            synchronized(queuedActions) {
                actions = null
            }
        }
    }
    private var isCreated = false

    var actions: RootActions? = null

    private val queuedActions = ArrayList<(ipc: RootActions) -> Unit>()

    fun onCreate() {
        if (!isCreated) {
            isCreated = true

            val script =
                    RootJava.getLaunchScript(context, RootHandler::class.java, null,
                            null, null, BuildConfig.APPLICATION_ID + ":root")

            isSuAsync {
                if (it) {
                    logicScope.launch {
                        Shell.SU.run(script.toTypedArray())
                    }
                }
            }
        }
    }

    fun onDestroy() {
        receiver.release()
        isCreated = false

        logicScope.launch {
            RootJava.cleanupCache(context)
        }
    }

    fun postAction(action: (ipc: RootActions) -> Unit) {
        synchronized(queuedActions) {
            if (actions == null) queuedActions.add(action)
            else action.invoke(actions!!)
        }
    }

}