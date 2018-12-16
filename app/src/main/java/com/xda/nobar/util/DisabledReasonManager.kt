package com.xda.nobar.util

/**
 * Base class for managing the reasons for why certain features are currently disabled
 */
open class DisabledReasonManager : HashSet<String>() {
    object NavBarReasons {
        const val NAV_BLACKLIST = "nav_blacklist"
        const val CAR_MODE = "car_mode"
        const val KEYGUARD = "keyguard"
        const val KEYBOARD = "keyboard"
    }

    object PillReasons {
        const val BLACKLIST = "bar_blacklist"
    }

    object ImmReasons {
        const val BLACKLIST = "imm_blacklist"
        const val EDGE_SCREEN = "edge"
    }
}