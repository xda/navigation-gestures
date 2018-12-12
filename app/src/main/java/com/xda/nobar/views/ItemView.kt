package com.xda.nobar.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckedTextView

class ItemView(context: Context, attributeSet: AttributeSet) : AppCompatCheckedTextView(context, attributeSet) {
    var name: String? = null
        set(value) {
            text = value
        }
    var value: String? = null
}