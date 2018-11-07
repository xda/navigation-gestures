package com.xda.nobar.util

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import java.io.IOException

class FlashlightControllerLollipop(override val context: Context) : FlashlightControllerBase(context) {
    override var flashlightEnabled: Boolean
        get() {
            return camera?.parameters?.flashMode == Camera.Parameters.FLASH_MODE_TORCH
        }
        set(value) {
            if (value) {
                camera = Camera.open()
                val parameters = camera?.parameters
                parameters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                camera?.parameters = parameters

                camera?.startPreview()
            } else {
                camera?.stopPreview()
                camera?.release()
                camera = null
            }
        }

    private var camera: Camera? = null

    override fun onCreate() {
        isCreated = true
    }

    override fun onDestroy() {
        isCreated = false
    }
}