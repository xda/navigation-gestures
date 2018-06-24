package com.xda.nobar.prefs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import com.pavelsikun.seekbarpreference.SeekBarPreferenceView
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnProgressSetListener
import java.util.*

/**
 * A combination of a SwitchPreference and a DialogPreference containing a SeekBarPreferenceView
 * The progress of the SeekBar is saved as ${key}_progress
 */
class SeekBarSwitchPreference(context: Context, attributeSet: AttributeSet) : SwitchPreference(context, attributeSet), OnProgressSetListener {
    companion object {
        const val KEY_SUFFIX = "_progress"
    }

    private val seekBar = SeekBarPreferenceView(context, attributeSet)
    private val dialog = Dialog(this, seekBar, this)

    init {
        dialog.setTitle(title)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        seekBar.currentValue = if (restoreValue) preferenceManager.sharedPreferences.getInt("$key$KEY_SUFFIX", seekBar.currentValue) else seekBar.currentValue
        super.onSetInitialValue(restoreValue, defaultValue)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        var switch = view.findViewById<Switch>(com.android.internal.R.id.switch_widget)
        if (switch == null) {
            switch = view.findViewById(context.resources.getIdentifier("switchWidget", "id", "android"))
        }

        switch.apply {
            setOnClickListener {  }
            setOnCheckedChangeListener { _, _ ->
                super.onClick()
            }
        }

        syncSummary()
    }

    override fun onProgressSet(progress: Int) {
        preferenceManager.sharedPreferences.edit().putInt("$key$KEY_SUFFIX", progress).apply()

        syncSummary()
    }

    override fun onClick() {
        if (isChecked) {
            dialog.show()
        } else super.onClick()
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)

        syncSummary()
    }

    fun getProgress() = preferenceManager.sharedPreferences.getInt("$key$KEY_SUFFIX", seekBar.currentValue)

    private fun syncSummary() {
        if (isChecked && summaryOn != null) summaryOn = String.format(Locale.getDefault(), context.resources.getString(R.string.auto_hide_pill_desc_enabled), getProgress())
    }

    class Dialog(private val preference: SeekBarSwitchPreference,
                 private val seekBar: SeekBarPreferenceView,
                 private val listener: OnProgressSetListener) : AlertDialog.Builder(preference.context), DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        init {
            setPositiveButton(android.R.string.ok, this)
            setNegativeButton(android.R.string.cancel, null)
            setOnCancelListener(this)
            setView(seekBar)
        }

        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> listener.onProgressSet(seekBar.currentValue)
            }
        }

        override fun onCancel(dialog: DialogInterface?) {
            seekBar.currentValue = preference.getProgress()
        }

        override fun show(): AlertDialog {
            if (seekBar.parent != null) {
                (seekBar.parent as ViewGroup).removeView(seekBar)
            }

            return super.show()
        }
    }
}