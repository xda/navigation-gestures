package com.xda.nobar.fragments.settings

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.*
import androidx.annotation.CallSuper
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.xda.nobar.util.actualParent
import com.xda.nobar.util.onClickListener
import com.xda.nobar.util.prefManager
import tk.zwander.collapsiblepreferencecategory.CollapsiblePreferenceCategory
import tk.zwander.collapsiblepreferencecategory.CollapsiblePreferenceGroupAdapter

abstract class BasePrefFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val PREF_KEY_TO_HIGHLIGHT = "key_to_highlight"
    }

    internal val prefManager by lazy { activity!!.prefManager }
    internal val prefToHighlight by lazy { arguments?.getString(PREF_KEY_TO_HIGHLIGHT) }

    abstract val resId: Int

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(resId, rootKey)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {}

    override fun onStart() {
        super.onStart()

        listView.post {
            prefToHighlight?.let {
                val pref = findPreference<Preference>(it) ?: return@let
                val parent = pref.actualParent

                if (parent is CollapsiblePreferenceCategory) parent.expanded = true
                if (pref is CollapsiblePreferenceCategory) pref.expanded = true

                listView.postDelayed({
                    scrollToPreference(it)
                    val view = findPreferenceView(it)

                    view?.let { v ->
                        v.isPressed = true
                        v.post { v.isPressed = false }
                    }
                }, 500)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return CollapsiblePreferenceGroupAdapter(preferenceScreen)
    }

    @SuppressLint("RestrictedApi")
    fun findPreferenceView(key: String): View? {
        for (i in 0 until listView.adapter!!.itemCount) {
            val item = (listView.adapter as PreferenceGroupAdapter).getItem(i)

            if (item.key == key) return listView.findViewHolderForAdapterPosition(i)?.itemView
        }

        return null
    }
}