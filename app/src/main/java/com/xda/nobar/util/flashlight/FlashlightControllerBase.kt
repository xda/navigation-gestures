package com.xda.nobar.util.flashlight

import android.content.Context

abstract class FlashlightControllerBase(internal open val context: Context) {
    abstract var flashlightEnabled: Boolean
    var isCreated = false

    open fun toggle() {
        flashlightEnabled = !flashlightEnabled
    }
    open fun onCreate(callback: (() -> Unit)? = null) {}
    open fun onDestroy(callback: (() -> Unit)? = null) {}
}