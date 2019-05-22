package com.xda.nobar.adapters.info

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ActionInfo(
        val label: CharSequence,
        val res: CharSequence?,
        val isHeader: Boolean
) : Parcelable