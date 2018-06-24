package com.xda.nobar.interfaces;

/**
 * Allow components to listen for changes in NoBar's nav hiding state
 */
public interface OnNavBarHideStateChangeListener {
    /**
     * Called when the navbar is shown or hidden
     * @param hidden true if the navbar was hidden by this change
     */
    void onNavStateChange(boolean hidden);
}
