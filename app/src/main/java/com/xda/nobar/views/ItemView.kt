package com.xda.nobar.views

import android.content.Context
import android.support.v7.widget.AppCompatCheckedTextView
import android.util.AttributeSet

class ItemView(context: Context, attributeSet: AttributeSet) : AppCompatCheckedTextView(context, attributeSet) {
    var name: String? = null
        set(value) {
            text = value
        }
    var value: String? = null
}