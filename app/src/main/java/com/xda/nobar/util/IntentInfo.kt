package com.xda.nobar.util

import android.os.Parcel
import android.os.Parcelable

class IntentInfo(
        val id: Int,
        var isChecked: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt() == 1)

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeInt(if (isChecked) 1 else 0)
    }

    override fun describeContents(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        return other is IntentInfo &&
                other.id == id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object CREATOR : Parcelable.Creator<IntentInfo> {
        override fun createFromParcel(parcel: Parcel): IntentInfo {
            return IntentInfo(parcel)
        }

        override fun newArray(size: Int): Array<IntentInfo?> {
            return arrayOfNulls(size)
        }
    }
}