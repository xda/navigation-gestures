package com.xda.nobar.fragments.settings

import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.view.animation.Animation
import androidx.annotation.CallSuper
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.xda.nobar.util.prefManager
import kotlinx.android.synthetic.main.activity_app_launch_select.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val anim = super.onCreateAnimation(transit, enter, nextAnim)

        if (enter) {
            anim?.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    highlight()
                }
            })

            if (anim == null) {
                highlight()
            }
        }

        return anim
    }

    private fun highlight() {
        listView?.postDelayed({
            prefToHighlight?.let {
                val pref = findPreference<Preference>(it) ?: return@let

                val parent = if (pref.parent?.parent is CollapsiblePreferenceCategory) pref.parent?.parent else pref.parent

                if (parent is CollapsiblePreferenceCategory) parent.expanded = true

                listView.postDelayed({
                    val position = (listView.adapter as PreferenceGroup.PreferencePositionCallback)
                            .getPreferenceAdapterPosition(it)

                    listView.smoothScrollToPosition(position)

                    val view = findPreferenceView(it)

                    view?.let { v ->
                        v.isPressed = true
                        v.postDelayed({ v.isPressed = false }, 500)
                    }
                }, 500)
            }
        }, 200)
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