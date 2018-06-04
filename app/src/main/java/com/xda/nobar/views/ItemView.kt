package com.xda.nobar.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckedTextView

class ItemView(context: Context, attributeSet: AttributeSet) : CheckedTextView(context, attributeSet) {
    var name: String? = null
        set(value) {
            text = value
        }
    var value: String? = null
}