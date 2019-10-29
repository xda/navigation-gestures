package com.xda.nobar.activities.helpers

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.createBitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.xda.nobar.util.mainHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Takes a screenshot, sort of
 * Before Android P, there are only two ways for a third party to take a screenshot:
 *     - Root
 *     - MediaProjection API
 * NoBar uses the latter (for non-rooted devices).
 *
 * MediaProjection is normally used for screen recording, but
 * by only capturing a single frame, we can essentially use it to get a screenshot.
 *
 * Since this isn't the native screenshot method, it won't trigger any screenshot tools, such as Samsung's Smart Capture.
 * It also may not work on every device.
 */
class ScreenshotActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val REQ = 100
        private const val PERM_REQ = 1000
    }

    private val projectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private val imageReader by lazy { ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1) }
    private val metrics = DisplayMetrics()
        get() {
            mainDisplay.getRealMetrics(field)
            return field
        }
    private val mainDisplay by lazy { windowManager.defaultDisplay }

    private val width: Int
        get() = metrics.widthPixels
    private val height: Int
        get() = metrics.heightPixels

    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var projection: MediaProjection

    private var count = 0

    private val params = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE
        else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT

        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    }

    private val flasher by lazy {
        LinearLayout(this@ScreenshotActivity).apply {
            setBackgroundColor(Color.WHITE)
        }
    }

    private val handlerThread = HandlerThread("NavGestScreenshot").apply {
        start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) getShot()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERM_REQ)
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()

        handlerThread.quitSafely()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        imageReader.setOnImageAvailableListener(ImageAvailableListener(), mainHandler)

        if (requestCode == REQ && data != null) {
            projection = projectionManager.getMediaProjection(resultCode, data)

            createVirtualDisplay()
            projection.registerCallback(MediaProjectionStopCallback(), mainHandler)
        } else {
            finish()
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
        launch {
            projection.stop()
        }

        finish()
    }

    private fun createVirtualDisplay() {
        virtualDisplay = projection.createVirtualDisplay("NavGestScreenShot",
                width, height,
                metrics.density.toInt(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.surface,
                null,
                mainHandler)
    }

    @Suppress("DEPRECATION")
    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            var image: Image? = null
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null

            try {
                if (count < 1) {
                    runOnUiThread {
                        try {
                            windowManager.addView(flasher, params)
                        } catch (e: Exception) {}

                        val listener = ValueAnimator.AnimatorUpdateListener {
                            flasher.alpha = it.animatedValue.toString().toFloat()
                        }

                        val exit = ValueAnimator.ofFloat(1f, 0f)
                        exit.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {}
                            override fun onAnimationStart(animation: Animator?) {}
                            override fun onAnimationCancel(animation: Animator?) {
                                onAnimationEnd(animation)
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                try {
                                    windowManager.removeView(flasher)
                                } catch (e: Exception) {}
                                stop()
                            }
                        })
                        exit.addUpdateListener(listener)
                        exit.interpolator = DecelerateInterpolator()
                        exit.duration = 150

                        val enter = ValueAnimator.ofFloat(0f, 1f)
                        enter.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {}
                            override fun onAnimationStart(animation: Animator?) {}
                            override fun onAnimationCancel(animation: Animator?) {
                                onAnimationEnd(animation)
                            }
                            override fun onAnimationEnd(animation: Animator?) {
                                exit.start()
                            }
                        })
                        enter.addUpdateListener(listener)
                        enter.interpolator = AccelerateInterpolator()
                        enter.duration = 150
                        enter.start()
                    }

                    image = reader.acquireNextImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width
                        val bitmapWidth = width + rowPadding / pixelStride

                        bitmap = createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                        bitmap.copyPixelsFromBuffer(buffer)

                        val cropped = Bitmap.createBitmap(bitmap!!, 0, 0, width, height)

                        // write bitmap to a file
                        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()

                        val file = File("$root/NavigationGestures")
                        file.mkdirs()

                        val dateFormat = SimpleDateFormat("yy_MM_dd_HHmm_ss", Locale.getDefault())
                        val screenshot = File(file, "Screenshot_${dateFormat.format(Date())}.jpg")

                        fos = FileOutputStream(screenshot)
                        cropped.compress(CompressFormat.JPEG, 100, fos)

                        MediaScannerConnection.scanFile(applicationContext, arrayOf(screenshot.toString()), null, null)

                        cropped.recycle()
                    }

                    count++
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
            }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            launch {
                virtualDisplay.release()
                imageReader.setOnImageAvailableListener(null, null)
                projection.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }
}
