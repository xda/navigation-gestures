package com.xda.nobar.util

class DisabledNavReasonManager : DisabledReasonManagerAbs() {
    companion object {
        const val NAV_BLACKLIST = "nav_blacklist"
        const val CAR_MODE = "car_mode"
        const val KEYGUARD = "keyguard"
        const val IMMERSIVE = "immersive"
        const val EDGE_SCREEN = "edge"
    }

    override val currentlyDisabled: ArrayList<String> = ArrayList()
}