package com.xda.nobar.util

class DisabledBarReasonManager : DisabledReasonManagerAbs() {
    companion object {
        const val BLACKLIST = "bar_blacklist"
    }

    override val currentlyDisabled: ArrayList<String> = ArrayList()
}