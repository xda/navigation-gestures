package com.xda.nobar.data

data class ColoredAppData(
        val packageName: String,
        val color: Int
) {
    override fun equals(other: Any?): Boolean {
        return other is ColoredAppData && other.packageName == packageName
    }

    override fun hashCode(): Int {
        return packageName.hashCode()
    }
}