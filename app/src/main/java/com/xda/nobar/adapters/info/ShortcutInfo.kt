package com.xda.nobar.adapters.info

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Parcel
import android.os.Parcelable

class ShortcutInfo(
        val activityInfo: ActivityInfo,
        var label: String,
        var isChecked: Boolean,
        var intent: Intent? = null
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(ActivityInfo::class.java.classLoader)!!,
            parcel.readString()!!,
            parcel.readByte() != 0.toByte(),
            parcel.readParcelable(Intent::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(activityInfo, flags)
        parcel.writeString(label)
        parcel.writeByte(if (isChecked) 1 else 0)
        parcel.writeParcelable(intent, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ShortcutInfo> {
        override fun createFromParcel(parcel: Parcel): ShortcutInfo {
            return ShortcutInfo(parcel)
        }

        override fun newArray(size: Int): Array<ShortcutInfo?> {
            return arrayOfNulls(size)
        }
    }
}