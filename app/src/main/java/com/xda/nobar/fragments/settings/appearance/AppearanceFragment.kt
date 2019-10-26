package com.xda.nobar.fragments.settings.appearance

import com.xda.nobar.R
import com.xda.nobar.fragments.settings.BasePrefFragment

/**
 * Appearance settings
 */
class AppearanceFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_appearance
    override val activityTitle by lazy { resources.getText(R.string.appearance) }
}