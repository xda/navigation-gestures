package com.xda.nobar.util

import java.util.*

/**
 * Base class for managing the reasons for why certain features are currently disabled
 */
open class DisabledReasonManager : TreeSet<String>() {
    object NavBarReasons {
        const val NAV_BLACKLIST = "nav_blacklist"
        const val CAR_MODE = "car_mode"
        const val KEYGUARD = "keyguard"
        const val IMMERSIVE = "immersive"
    }

    object PillReasons {
        const val BLACKLIST = "bar_blacklist"
    }

    object ImmReasons {
        const val BLACKLIST = "imm_blacklist"
        const val EDGE_SCREEN = "edge"
    }

    private val lock = Any()

    override fun add(element: String): Boolean {
        return if (!contains(element)) super.add(element)
        else false
    }

    fun removeAll(element: String): Boolean {
        synchronized(lock) {
            return removeAll(Collections.singleton(element))
        }
    }
}