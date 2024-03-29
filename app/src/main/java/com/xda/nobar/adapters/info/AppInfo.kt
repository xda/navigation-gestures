package com.xda.nobar.adapters.info

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Simple helper class that contains relevant app information
 * For use by BaseAppSelectActivity
 * @param packageName the package name of the app
 * @param activity the component name of the target activity
 * @param displayName the display name of the app
 * @param isChecked whether or not this item should appear as checked in a list
 */
@Parcelize
open class AppInfo(
        val packageName: String,
        val activity: String,
        val displayName: String,
        val icon: Int,
        var isChecked: Boolean
) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return (
                other is AppInfo &&
                        other.packageName == packageName &&
                        other.activity == activity &&
                        other.displayName == displayName &&
                        other.icon == icon
                )
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + activity.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + icon
        result = 31 * result + isChecked.hashCode()
        return result
    }
}