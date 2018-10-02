package com.xda.nobar.interfaces

import android.content.Intent

interface ReceiverCallback {
    fun onActionReceived(intent: Intent?)
}