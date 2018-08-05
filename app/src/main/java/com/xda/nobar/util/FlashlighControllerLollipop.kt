package com.xda.nobar.util

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import java.io.IOException

class FlashlighControllerLollipop(override val context: Context) : FlashlightControllerBase(context) {
    override var flashlightEnabled: Boolean
        get() {
            return camera.parameters.flashMode == Camera.Parameters.FLASH_MODE_TORCH
        }
        set(value) {
            if (value) {
                val parameters = camera.parameters
                parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                camera.parameters = parameters

                try {
                    camera.setPreviewTexture(texture)
                } catch (e: IOException) {}

                camera.startPreview()
            } else {
                camera.stopPreview()
            }
        }

    private val camera by lazy { Camera.open() }
    private var texture: SurfaceTexture? = null

    override fun onDestroy() {
        camera.stopPreview()
    }
}