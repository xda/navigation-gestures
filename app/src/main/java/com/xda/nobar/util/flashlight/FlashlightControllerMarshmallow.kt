package com.xda.nobar.util.flashlight

import android.annotation.TargetApi
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler

@TargetApi(Build.VERSION_CODES.M)
class FlashlightControllerMarshmallow(override val context: Context) : FlashlightControllerBase(context) {
    override var flashlightEnabled: Boolean
        set(value) {
            try {
                manager.setTorchMode(cameraId ?: return, value)
            } catch (e: Exception) {}
            flashlightEnabledInternal = value
        }
        get() = flashlightEnabledInternal

    private var flashlightEnabledInternal = false

    private val cameraId: String?
        @Throws(CameraAccessException::class)
        get() {
            for (id in manager.cameraIdList) {
                try {
                    val c = manager.getCameraCharacteristics(id)
                    val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: return null
                    val lensFacing = c.get(CameraCharacteristics.LENS_FACING) ?: return null

                    if (flashAvailable && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        return id
                    }
                } catch (e: Exception) {}
            }
            return null
        }

    private val manager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(cameraId: String) {}

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (this@FlashlightControllerMarshmallow.cameraId == cameraId) {
                flashlightEnabledInternal = enabled
            }
        }
    }

    override fun onCreate(callback: (() -> Unit)?) {
        manager.registerTorchCallback(this.callback, Handler())
        isCreated = true
        callback?.invoke()
    }

    override fun onDestroy(callback: (() -> Unit)?) {
        if (isCreated) {
            try {
                manager.setTorchMode(cameraId!!, false)
            } catch (e: Exception) {}

            manager.unregisterTorchCallback(this.callback)
            isCreated = false
            callback?.invoke()
        }
    }
}