package com.xda.nobar.util.helpers

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
        const val CAR_MODE = "bar_car_mode"
        const val LOCK_SCREEN = "lock_screen"
        const val PERMISSIONS = "permissions"
        const val INSTALLER = "installer"
        const val SCREEN_OFF = "screen_off"
    }

    object ImmReasons {
        const val BLACKLIST = "imm_blacklist"
        const val EDGE_SCREEN = "edge"
    }
}