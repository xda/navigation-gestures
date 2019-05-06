package com.xda.nobar.fragments.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xda.nobar.R
import kotlinx.android.synthetic.main.slide_welcome.*

/**
 * Similar to WelcomeFragment but with a different video
 */
class WriteSecureFragment : WelcomeFragment() {
    override val videoRes = R.raw.hide_nav

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.slide_welcome, container, false)
    }

    override fun viewCreated() {
        mi_title?.text = resources.getText(R.string.write_secure_settings)
        mi_description?.text = resources.getText(R.string.write_secure_settings_desc)
    }
}