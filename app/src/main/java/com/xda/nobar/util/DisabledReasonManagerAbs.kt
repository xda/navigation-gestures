package com.xda.nobar.util

abstract class DisabledReasonManagerAbs {
    abstract val currentlyDisabled: ArrayList<String>

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