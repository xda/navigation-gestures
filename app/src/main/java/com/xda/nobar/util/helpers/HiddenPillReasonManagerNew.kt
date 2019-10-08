package com.xda.nobar.util.helpers

import java.util.*

class HiddenPillReasonManagerNew : TreeMap<Long, String>() {
    companion object {
        const val AUTO = "auto"
        const val FULLSCREEN = "fullscreen"
        const val KEYBOARD = "keyboard"
        const val MANUAL = "manual"
    }

    fun getMostRecentReason(): String? {
        return lastEntry()?.value
    }

    fun removeReason(reason: String) {
        //Singleton collection to remove all
        values.removeAll(Collections.singleton(reason))
    }

    fun addReason(reason: String) {
        val time = System.currentTimeMillis()

        put(time, reason)
    }
}