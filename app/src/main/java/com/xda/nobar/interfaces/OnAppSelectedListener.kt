package com.xda.nobar.interfaces

import com.xda.nobar.activities.AppLaunchSelectActivity

interface OnAppSelectedListener {
    fun onAppSelected(info: AppLaunchSelectActivity.AppInfo)
}