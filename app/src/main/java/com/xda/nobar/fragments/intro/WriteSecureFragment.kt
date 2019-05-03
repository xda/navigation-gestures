package com.xda.nobar.fragments.intro

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import com.xda.nobar.R
import com.xda.nobar.util.setup
import kotlinx.android.synthetic.main.slide_welcome.*

/**
 * Similar to WelcomeFragment but with a different video
 */
class WriteSecureFragment : SlideFragment() {
    private var setupVideoView = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.slide_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mi_title.text = resources.getText(R.string.write_secure_settings)
        mi_description.text = resources.getText(R.string.write_secure_settings_desc)

        Log.e("NoBar", setupVideoView.toString())

        if (!setupVideoView) {
            setupVideoView = true
            mi_image.setup(Uri.parse("android.resource://${context?.packageName}/${R.raw.hide_nav}"))
        }
    }
}