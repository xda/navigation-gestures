package com.xda.nobar.util

import android.os.Handler
import android.os.Looper

class LogicHandler(looper: Looper) : Handler(looper) {
    fun postLogged(runnable: () -> Unit): Boolean {
        return post(runnable)
    }
}