package com.xda.nobar.util

import android.content.Context

abstract class FlashlightControllerBase(internal open val context: Context) {
    abstract var flashlightEnabled: Boolean

    open fun onCreate() {}
    open fun onDestroy() {}
}