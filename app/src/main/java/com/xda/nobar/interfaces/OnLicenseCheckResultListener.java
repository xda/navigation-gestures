package com.xda.nobar.interfaces;

/**
 * Allows components to listen for changes in the premium state
 */
public interface OnLicenseCheckResultListener {
    /**
     * Called when a result on a license check is available
     * @param valid if the user has valid premium
     * @param reason the explanation of the state of "valid"
     */
    void onResult(boolean valid, String reason);
}
