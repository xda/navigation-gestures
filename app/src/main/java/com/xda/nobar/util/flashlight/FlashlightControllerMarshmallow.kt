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
    override var flashlightEnabled = false
        set(value) {
            try {
                manager.setTorchMode(cameraId ?: return, value)
            } catch (e: Exception) {}
            field = value
        }
        get() = flashlightEnabledInternal

    private var flashlightEnabledInternal = false

    private val manager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(cameraId: String) {}

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            flashlightEnabledInternal = enabled
        }
    }

    private val cameraId: String?
        @Throws(CameraAccessException::class)
        get() {
            for (id in manager.cameraIdList) {
                try {
                    val c = manager.getCameraCharacteristics(id)
                    val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    val lensFacing = c.get(CameraCharacteristics.LENS_FACING)
                    if (flashAvailable != null && flashAvailable && lensFacing != null && lensFacing == 1) {
                        return id
                    }
                } catch (e: Exception) {}
            }
            return null
        }

    override fun onCreate(callback: (() -> Unit)?) {
        manager.registerTorchCallback(this.callback, Handler())
        isCreated = true
        callback?.invoke()
    }

    override fun onDestroy(callback: (() -> Unit)?) {
        try {
            manager.setTorchMode(cameraId!!, false)
        } catch (e: Exception) {}

        manager.unregisterTorchCallback(this.callback)
        isCreated = false
        callback?.invoke()
    }
}