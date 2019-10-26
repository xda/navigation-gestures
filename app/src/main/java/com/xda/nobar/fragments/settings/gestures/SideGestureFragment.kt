package com.xda.nobar.fragments.settings.gestures

import com.xda.nobar.R

class SideGestureFragment : GestureFragment() {
    override val resId = R.xml.prefs_side_gestures
    override val activityTitle by lazy { resources.getText(R.string.side_gestures) }
}