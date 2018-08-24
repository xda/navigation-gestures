package com.xda.nobar.util

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
            field = value
            try {
                manager.setTorchMode(cameraId!!, value)
            } catch (e: CameraAccessException) {}
        }

    private val manager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(cameraId: String) {}

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            flashlightEnabled = enabled
        }
    }

    private val cameraId: String?
        @Throws(CameraAccessException::class)
        get() {
            for (id in manager.cameraIdList) {
                val c = manager.getCameraCharacteristics(id)
                val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val lensFacing = c.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable != null && flashAvailable && lensFacing != null && lensFacing == 1) {
                    return id
                }
            }
            return null
        }

    override fun onCreate() {
        manager.registerTorchCallback(callback, Handler())
    }

    override fun onDestroy() {
        manager.setTorchMode(cameraId, false)
        manager.unregisterTorchCallback(callback)
    }
}