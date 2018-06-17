package com.xda.nobar.util

object DisabledReasonManager {
    const val NAV_BLACKLIST = "nav_blacklist"
    const val CAR_MODE = "car_mode"
    const val KEYGUARD = "keyguard"
    const val IMMERSIVE = "immersive"
    const val EDGE_SCREEN = "edge"

    private val currentlyDisabled = ArrayList<String>()

    fun add(value: String) {
        currentlyDisabled.add(value)
    }

    fun remove(value: String) {
        currentlyDisabled.remove(value)
    }

    fun contains(value: String): Boolean {
        return currentlyDisabled.contains(value)
    }

    fun isEmpty(): Boolean {
        return currentlyDisabled.isEmpty()
    }
}