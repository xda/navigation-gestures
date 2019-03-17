package com.xda.nobar.adapters.info

import android.content.Intent
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ShortcutInfo(
        val clazz: String,
        val packageName: String,
        val icon: Int,
        var label: String,
        var isChecked: Boolean,
        var intent: Intent? = null
): Parcelable {
    override fun describeContents(): Int {
        return 0
    }
}