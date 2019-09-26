package com.xda.nobar.interfaces;

import com.xda.nobar.adapters.info.AppInfo;

/**
 * For use in any BaseAppSelectActivity
 * Called when the user selects an app
 * Should be passed into AppSelectAdapter's constructor
 */
public interface OnAppSelectedListener {
    /**
     * Called when the user selects an app
     * @param info the information corresponding to the selected app
     */
    void onAppSelected(AppInfo info, boolean isChecked);
}
