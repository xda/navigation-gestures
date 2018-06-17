package com.xda.nobar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.Display
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ScreenshotActivity : AppCompatActivity() {
    companion object {
        private const val REQ = 100
        private const val PERM_REQ = 1000
    }

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var imageReader: ImageReader
    private lateinit var mainDisplay: Display
    private lateinit var handler: Handler
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var projection: MediaProjection

    private var width = 0
    private var height = 0
    private var count = 0

    private val handlerThread = HandlerThread("NavGestScreenshot").apply {
        start()
        handler = Handler(looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mainDisplay = windowManager.defaultDisplay

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) getShot()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERM_REQ)
    }

    override fun onDestroy() {
        super.onDestroy()

        handlerThread.quitSafely()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ) {
            projection = projectionManager.getMediaProjection(resultCode, data)

            createVirtualDisplay()
            projection.registerCallback(MediaProjectionStopCallback(), handler)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERM_REQ) {
            if (!grantResults.contains(PackageManager.PERMISSION_GRANTED)) finish()
            else getShot()
        }
    }

    private fun getShot() {
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQ)
    }

    private fun stop() {
        handler.post {
            projection.stop()
        }

        finish()
    }

    private fun createVirtualDisplay() {
        val size = Point()
        mainDisplay.getSize(size)

        width = size.x
        height = size.y

        val metrics = DisplayMetrics()
        mainDisplay.getMetrics(metrics)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        virtualDisplay = projection.createVirtualDisplay("NavGestScreenShot",
                width, height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                imageReader.surface,
                null,
                handler)

        imageReader.setOnImageAvailableListener(ImageAvailableListener(), handler)
    }

    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            var image: Image? = null
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null

            try {
                if (count < 1) {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        // create bitmap
                        bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                        bitmap.copyPixelsFromBuffer(buffer)

                        // write bitmap to a file
                        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()

                        val file = File("$root/NavigationGestures")
                        file.mkdirs()

                        val dateFormat = SimpleDateFormat("yy_MM_dd_HHmm_ss", Locale.getDefault())
                        val screenshot = File(file, "Screenshot_${dateFormat.format(Date())}.jpg")

                        fos = FileOutputStream(screenshot)
                        bitmap.compress(CompressFormat.JPEG, 100, fos)

                        MediaScannerConnection.scanFile(this@ScreenshotActivity, arrayOf(screenshot.toString()), null, null)

                        count++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    fos?.close()
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }

                bitmap?.recycle()

                image?.close()

                stop()
            }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            handler.post {
                virtualDisplay.release()
                imageReader.setOnImageAvailableListener(null, null)
                projection.unregisterCallback(this)
            }
        }
    }
}
