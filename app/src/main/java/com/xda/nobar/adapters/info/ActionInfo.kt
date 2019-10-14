package com.xda.nobar.adapters.info

import android.os.Parcel
import android.os.Parcelable

class ActionInfo(
        val label: CharSequence,
        val res: CharSequence?,
        val isHeader: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label.toString())
        parcel.writeString(res?.toString())
        parcel.writeByte(if (isHeader) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ActionInfo> {
        override fun createFromParcel(parcel: Parcel): ActionInfo {
            return ActionInfo(parcel)
        }

        override fun newArray(size: Int): Array<ActionInfo?> {
            return arrayOfNulls(size)
        }
    }
}