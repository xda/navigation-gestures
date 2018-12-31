package com.xda.nobar.adapters.info

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable

class ShortcutInfo(
        val clazz: String,
        val packageName: String,
        val icon: Int,
        var label: String,
        var isChecked: Boolean,
        var intent: Intent? = null
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readInt(),
            parcel.readString()!!,
            parcel.readByte() != 0.toByte(),
            parcel.readParcelable(Intent::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(clazz)
        parcel.writeString(packageName)
        parcel.writeInt(icon)
        parcel.writeString(label)
        parcel.writeByte(if (isChecked) 1 else 0)
        parcel.writeParcelable(intent?.cloneFilter(), flags)
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