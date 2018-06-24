package com.xda.nobar.interfaces;

import com.xda.nobar.views.BarView;

/**
 * Allow components to listen for changes in NoBar's pill state
 */
public interface OnGestureStateChangeListener {
    /**
     * Called when the pill is activated or deactivated
     * @param barView the pill view
     * @param activated whether the pill is onscreen or not
     */
    void onGestureStateChange(BarView barView, boolean activated);
}
