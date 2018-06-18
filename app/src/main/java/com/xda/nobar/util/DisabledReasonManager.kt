package com.xda.nobar.util

import java.util.*

open class DisabledReasonManager : TreeSet<String>() {
    object NavBarReasons {
        const val NAV_BLACKLIST = "nav_blacklist"
        const val CAR_MODE = "car_mode"
        const val KEYGUARD = "keyguard"
        const val IMMERSIVE = "immersive"
        const val EDGE_SCREEN = "edge"
    }

    object PillReasons {
        const val BLACKLIST = "bar_blacklist"
    }

    override fun add(element: String): Boolean {
        return if (!contains(element)) super.add(element)
        else false
    }

    override fun remove(element: String): Boolean {
        return removeAll(Collections.singleton(element))
    }

    fun removeFirst(element: String): Boolean {
        return super.remove(element)
    }
}