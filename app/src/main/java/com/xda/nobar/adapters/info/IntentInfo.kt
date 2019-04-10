package com.xda.nobar.adapters.info

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class IntentInfo(
        val id: Int,
        val res: Int,
        var isChecked: Boolean
) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return other is IntentInfo &&
                other.id == id
    }

    override fun hashCode(): Int {
        return id
    }
}