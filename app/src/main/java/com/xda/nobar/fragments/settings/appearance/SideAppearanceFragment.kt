package com.xda.nobar.fragments.settings.appearance

import com.xda.nobar.R
import com.xda.nobar.fragments.settings.BasePrefFragment
import com.xda.nobar.util.app

class SideAppearanceFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_side_appearance
    override val activityTitle by lazy { resources.getText(R.string.side_appearance) }

    override fun onResume() {
        super.onResume()

        requireContext().app.apply {
            leftSide.isShowing = true
            rightSide.isShowing = true
        }
    }

    override fun onPause() {
        super.onPause()

        requireContext().app.apply {
            leftSide.isShowing = false
            rightSide.isShowing = false
        }
    }
}