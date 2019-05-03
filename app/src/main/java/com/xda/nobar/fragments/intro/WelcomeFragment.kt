package com.xda.nobar.fragments.intro

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import com.xda.nobar.R
import kotlinx.android.synthetic.main.slide_welcome.*

/**
 * The first slide: show a welcome
 * Uses a custom layout to show a video instead of an image
 */
open class WelcomeFragment : SlideFragment(), TextureView.SurfaceTextureListener {
    internal open val videoRes = R.raw.nav_gesture

    internal val mediaPlayer by lazy { MediaPlayer.create(context, videoRes) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.slide_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mi_image.surfaceTextureListener = this
        viewCreated()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val s = Surface(surface)

        mediaPlayer.setSurface(s)
        mediaPlayer.start()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return false
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

    internal open fun viewCreated() {
        mi_title.text = resources.getText(R.string.welcome)
        mi_description.text = resources.getText(R.string.app_purpose)
    }
}