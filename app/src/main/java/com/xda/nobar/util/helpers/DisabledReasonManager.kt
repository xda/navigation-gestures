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
        const val FULLSCREEN = "fullscreen"
        const val VOLUME_LANDSCAPE = "volume"
        const val APP_PINNED = "app_pinned"
    }

    object PillReasons {
        const val BLACKLIST = "bar_blacklist"
        const val CAR_MODE = "bar_car_mode"
        const val LOCK_SCREEN = "lock_screen"
        const val PERMISSIONS = "permissions"
        const val INSTALLER = "installer"
        const val SCREEN_OFF = "screen_off"
        const val HIDE_DIALOG = "hide_dialog"
    }

    object ImmReasons {
        const val BLACKLIST = "imm_blacklist"
        const val EDGE_SCREEN = "edge"
    }

    object BlackoutReasons {
        const val KEYGUARD = "keyguard"
        const val KEYBOARD = "keyboard"
    }

    fun setConditional(reason: String, condition: (reason: String) -> Boolean) {
        synchronized(this) {
            if (condition(reason)) add(reason)
            else remove(reason)
        }
    }

    override fun add(element: String): Boolean {
        synchronized(this) {
            return super.add(element)
        }
    }

    override fun remove(element: String): Boolean {
        synchronized(this) {
            return super.remove(element)
        }
    }

    override fun addAll(elements: Collection<String>): Boolean {
        synchronized(this) {
            return super.addAll(elements)
        }
    }

    override fun removeAll(elements: Collection<String>): Boolean {
        synchronized(this) {
            return super.removeAll(elements)
        }
    }

    override fun clear() {
        synchronized(this) {
            super.clear()
        }
    }
}