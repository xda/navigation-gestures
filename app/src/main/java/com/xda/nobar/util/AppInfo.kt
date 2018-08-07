package com.xda.nobar.util

import android.os.Parcel
import android.os.Parcelable

/**
 * Simple helper class that contains relevant app information
 * For use by BaseAppSelectActivity
 * @param packageName the package name of the app
 * @param activity the component name of the target activity
 * @param displayName the display name of the app
 * @param isChecked whether or not this item should appear as checked in a list
 */
class AppInfo(val packageName: String, val activity: String, val displayName: String, val icon: Int, var isChecked: Boolean) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt() == 1)

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeString(activity)
        dest.writeString(displayName)
        dest.writeInt(icon)
        dest.writeInt(if (isChecked) 1 else 0)
    }

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

    companion object CREATOR : Parcelable.Creator<AppInfo> {
        override fun createFromParcel(parcel: Parcel): AppInfo {
            return AppInfo(parcel)
        }

        override fun newArray(size: Int): Array<AppInfo?> {
            return arrayOfNulls(size)
        }
    }
}