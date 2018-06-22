package com.xda.nobar.util

import java.util.*

class HiddenPillReasonManager : ArrayList<String>() {
    companion object {
        const val AUTO = "auto"
        const val FULLSCREEN = "fullscreen"
        const val KEYBOARD = "keyboard"
    }
    override fun add(element: String): Boolean {
        return if (contains(element)) {
            if (getMostRecentReason() == element) false
            else moveToRecent(element)
        }
        else super.add(element)
    }

    fun removeAll(element: String): Boolean {
        return removeAll(Collections.singleton(element))
    }

    fun moveToRecent(element: String): Boolean {
        return if (!contains(element)) false
        else {
            removeAll(element)
            add(element)
        }
    }

    fun getMostRecentReason(): String {
        return get(lastIndex)
    }
}