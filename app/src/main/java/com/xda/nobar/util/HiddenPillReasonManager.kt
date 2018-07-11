package com.xda.nobar.util

import org.apache.commons.collections4.list.SetUniqueList
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * Manager to handle the reasons why the pill is currently in its "hide" state
 */
class HiddenPillReasonManager : SetUniqueList<String>(ArrayList<String>(), HashSet<String>()) {
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

    @Deprecated("Not needed", ReplaceWith("remove(element)"))
    fun removeAll(element: String): Boolean {
        return remove(element)
    }

    fun moveToRecent(element: String): Boolean {
        return if (!contains(element)) false
        else {
            remove(element)
            add(element)
        }
    }

    fun getMostRecentReason(): String {
        var ret = ""
        while (ret.isEmpty()) {
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