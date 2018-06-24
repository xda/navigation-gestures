package com.xda.nobar.interfaces;

/**
 * Used by SeekBarSwitchPreference to listen for changes in the dialog SeekBar's progress
 */
public interface OnProgressSetListener {
    /**
     * Called when the user presses "OK" on the dialog
     * @param progress the progress of the dialog SeekBar (not scaled)
     */
    void onProgressSet(int progress);
}
