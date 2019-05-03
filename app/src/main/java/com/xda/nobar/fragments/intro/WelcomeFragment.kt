package com.xda.nobar.fragments.intro

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import com.xda.nobar.R
import com.xda.nobar.util.setup
import kotlinx.android.synthetic.main.slide_welcome.*

/**
 * The first slide: show a welcome
 * Uses a custom layout to show a video instead of an image
 */
class WelcomeFragment : SlideFragment() {
    private var setupVideoView = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.slide_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mi_title.text = resources.getText(R.string.welcome)
        mi_description.text = resources.getText(R.string.app_purpose)

        if (!setupVideoView) {
            setupVideoView = true
            mi_image.setup(Uri.parse("android.resource://${context?.packageName}/${R.raw.nav_gesture}"))
        }
    }
}