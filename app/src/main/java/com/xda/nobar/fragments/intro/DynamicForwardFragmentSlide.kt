package com.xda.nobar.fragments.intro

import com.heinrichreimersoftware.materialintro.slide.FragmentSlide

/**
 * Same as DynamicForwardSlide but for FragmentSlides
 */
class DynamicForwardFragmentSlide(builder: FragmentSlide.Builder, private val action: () -> Boolean) : FragmentSlide(builder) {
    override fun canGoForward(): Boolean {
        return action.invoke()
    }
}