package com.xda.nobar.util.flashlight

import android.content.Context

abstract class FlashlightControllerBase(internal open val context: Context) {
    abstract var flashlightEnabled: Boolean
    var isCreated = false

    open fun onCreate() {}
    open fun onDestroy() {}
}