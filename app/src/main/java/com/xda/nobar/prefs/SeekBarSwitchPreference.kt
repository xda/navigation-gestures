package com.xda.nobar.prefs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.Switch
import androidx.preference.AndroidResources
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnProgressSetListener
import com.xda.nobar.util.dpAsPx
import com.xda.nobar.util.prefManager
import tk.zwander.seekbarpreference.SeekBarView

/**
 * A combination of a SwitchPreference and a DialogPreference containing a SeekBarView
 * The progress of the SeekBar is saved as ${key}_progress
 */
class SeekBarSwitchPreference(context: Context, attributeSet: AttributeSet) : SwitchPreference(context, attributeSet), OnProgressSetListener {
    companion object {
        const val KEY_SUFFIX = "_progress"
    }

    private val seekBar = object : SeekBarView(context, attributeSet) {
        init {
            setPaddingRelative(context.dpAsPx(16), 0, context.dpAsPx(16), 0)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()

            layoutParams.apply {
                height = context.dpAsPx(100)

                layoutParams = this
            }
        }
    }
    private val dialog = Dialog(this, seekBar, this)

    init {
        dialog.setTitle(title)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        seekBar.setValue(if (isPersistent) context.prefManager.getInt("$key$KEY_SUFFIX",
                seekBar.getCurrentProgress()).toFloat() else seekBar.getCurrentProgress().toFloat(),
                true)
        super.onSetInitialValue(defaultValue)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val switch = holder.itemView.findViewById<Switch>(AndroidResources.ANDROID_R_SWITCH_WIDGET)

        switch.apply {
            setOnClickListener {  }
            setOnCheckedChangeListener { _, _ ->
                super.onClick()
            }
        }
    }

    override fun onAttached() {
        super.onAttached()

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

    fun getProgress() = context.prefManager.getInt("$key$KEY_SUFFIX", seekBar.getCurrentProgress())

    private fun syncSummary() {
        if (isChecked && summaryOn != null)
            summaryOn = context.resources.getString(R.string.auto_hide_pill_desc_enabled, getProgress().toString())
    }

    class Dialog(private val preference: SeekBarSwitchPreference,
                 private val seekBar: SeekBarView,
                 private val listener: OnProgressSetListener) : AlertDialog.Builder(preference.context), DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        init {
            setPositiveButton(android.R.string.ok, this)
            setNegativeButton(android.R.string.cancel, null)
            setOnCancelListener(this)
            setView(seekBar)
        }

        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> listener.onProgressSet(seekBar.getCurrentProgress())
            }
        }

        override fun onCancel(dialog: DialogInterface?) {
            seekBar.setValue(preference.getProgress().toFloat(), true)
        }

        override fun show(): AlertDialog {
            if (seekBar.parent != null) {
                (seekBar.parent as ViewGroup).removeView(seekBar)
            }

            return super.show()
        }
    }
}