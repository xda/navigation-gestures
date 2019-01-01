package com.xda.nobar.fragments.intro

import com.heinrichreimersoftware.materialintro.slide.SimpleSlide

/**
 * The library only checks once if the user can go forward in the simple builder
 * so we need to wrap that builder
 */
class DynamicForwardSlide(builder: SimpleSlide.Builder, private val action: () -> Boolean) : SimpleSlide(builder) {
    override fun canGoForward(): Boolean {
        return action.invoke()
    }
}