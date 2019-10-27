package com.xda.nobar.fragments.settings.appearance

import com.xda.nobar.R
import com.xda.nobar.fragments.settings.BasePrefFragment

class SideAppearanceFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_side_appearance
    override val activityTitle by lazy { resources.getText(R.string.side_appearance) }
}