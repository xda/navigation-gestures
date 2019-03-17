package com.xda.nobar.activities

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
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
class ScreenshotActivity : AppCompatActivity() {
    companion object {
        private const val REQ = 100
        private const val PERM_REQ = 1000
    }

    private val projectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private val imageReader by lazy { ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1) }
    private val size = Point()
        get() {
            mainDisplay.getRealSize(field)
            return field
        }
    private val metrics = DisplayMetrics()
        get() {
            mainDisplay.getMetrics(field)
            return field
        }
    private val mainDisplay by lazy { windowManager.defaultDisplay }
    private val handler by lazy { Handler(handlerThread.looper) }

    private val width: Int
        get() = size.x
    private val height: Int
        get() = size.y

    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var projection: MediaProjection

    private var count = 0

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

        handlerThread.quitSafely()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ && data != null) {
            projection = projectionManager.getMediaProjection(resultCode, data)

            createVirtualDisplay()
            projection.registerCallback(MediaProjectionStopCallback(), handler)
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
        handler.post {
            projection.stop()
        }

        finish()
    }

    private fun createVirtualDisplay() {
        virtualDisplay = projection.createVirtualDisplay("NavGestScreenShot",
                width, height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.surface,
                null,
                handler)

        imageReader.setOnImageAvailableListener(ImageAvailableListener(), handler)
    }

    @Suppress("DEPRECATION")
    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            var image: Image? = null
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null

            try {
                if (count < 1) { //make sure we only get one image
                    runOnUiThread {
                        val flasher = LinearLayout(this@ScreenshotActivity).apply {
                            setBackgroundColor(Color.WHITE)
                        }

                        val params = WindowManager.LayoutParams().apply {
                            type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE
                            else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

                            width = WindowManager.LayoutParams.MATCH_PARENT
                            height = WindowManager.LayoutParams.MATCH_PARENT

                            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN or
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

                            alpha = 0f
                        }

                        windowManager.addView(flasher, params)

                        val listener = ValueAnimator.AnimatorUpdateListener {
                            params.alpha = it.animatedValue.toString().toFloat()
                            runOnUiThread { windowManager.updateViewLayout(flasher, params) }
                        }

                        val exit = ValueAnimator.ofFloat(1f, 0f)
                        exit.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {}
                            override fun onAnimationStart(animation: Animator?) {}
                            override fun onAnimationCancel(animation: Animator?) {
                                onAnimationEnd(animation)
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                runOnUiThread { windowManager.removeView(flasher) }
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

                        MediaScannerConnection.scanFile(applicationContext, arrayOf(screenshot.toString()), null, null)

                        count++
                    }
                } else return
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