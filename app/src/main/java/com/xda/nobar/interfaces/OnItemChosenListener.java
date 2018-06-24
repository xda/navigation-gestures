package com.xda.nobar.interfaces;

import android.annotation.Nullable;

/**
 * For use by SectionableListPreference
 * Used to listen for the selection of an item in the list
 */
public interface OnItemChosenListener {
    /**
     * Called when the user selects an item
     * @param value the value/key of the item selected
     */
    void onItemChosen(@Nullable String value);
}
