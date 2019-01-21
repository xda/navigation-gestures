package com.xda.nobar.util.helpers

import java.util.*

/**
 * Manager to handle the reasons why the pill is currently in its "hide" state
 */
class HiddenPillReasonManager : ArrayList<String>() {
    companion object {
        const val AUTO = "auto"
        const val FULLSCREEN = "fullscreen"
        const val KEYBOARD = "keyboard"
        const val MANUAL = "manual"
    }
    override fun add(element: String): Boolean {
        return if (contains(element)) {
            if (getMostRecentReason() == element) false
            else moveToRecent(element)
        } else super.add(element)
    }

    override fun remove(element: String): Boolean {
        return if (!contains(element)) false
        else removeAll(Collections.singletonList(element))
    }

    fun moveToRecent(element: String): Boolean {
        return if (!contains(element)) false
        else {
            remove(element)
            add(element)
        }
    }

    fun getMostRecentReason(): String? {
        var ret: String? = null
        while (ret.isNullOrEmpty()) {
            val get = try {
                get(size - 1)
            } catch (e: ArrayIndexOutOfBoundsException) {
                null
            }

            if (get != null) {
                ret = get
                break
            }
        }

        return ret
    }

    fun onlyContains(element: String): Boolean {
        return if (!contains(element)) false
        else size == 1
    }
}