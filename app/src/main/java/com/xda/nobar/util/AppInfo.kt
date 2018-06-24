package com.xda.nobar.util

import android.graphics.drawable.Drawable

/**
 * Simple helper class that contains relevant app information
 * For use by BaseAppSelectActivity
 * @param packageName the package name of the app
 * @param activity the component name of the target activity
 * @param displayName the display name of the app
 * @param isChecked whether or not this item should appear as checked in a list
 */
class AppInfo(val packageName: String, val activity: String, val displayName: String, val icon: Drawable, var isChecked: Boolean)