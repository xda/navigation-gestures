package com.xda.nobar.util

import org.apache.commons.collections4.list.SetUniqueList
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * Base class for managing the reasons for why certain features are currently disabled
 */
open class DisabledReasonManager : SetUniqueList<String>(ArrayList<String>(), HashSet<String>()) {
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

    @Deprecated("Not needed", ReplaceWith("remove(element)"))
    fun removeAll(element: String): Boolean {
        return remove(element)
    }
}