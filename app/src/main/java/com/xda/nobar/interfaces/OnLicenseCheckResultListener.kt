package com.xda.nobar.interfaces

interface OnLicenseCheckResultListener {
    fun onLicenseCheckResult(valid: Boolean, reason: String)
}