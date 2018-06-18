package com.xda.nobar.util

abstract class DisabledReasonManagerAbs {
    internal abstract val currentlyDisabled: ArrayList<String>

    fun add(value: String) {
        if (!contains(value)) currentlyDisabled.add(value)
    }

    fun remove(value: String) {
        while (contains(value)) currentlyDisabled.remove(value)
    }

    fun contains(value: String): Boolean {
        return currentlyDisabled.contains(value)
    }

    fun isEmpty(): Boolean {
        return currentlyDisabled.isEmpty()
    }
}