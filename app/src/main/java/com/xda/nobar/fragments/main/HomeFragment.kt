package com.xda.nobar.fragments.main

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.ui.IntroActivity
import com.xda.nobar.activities.ui.TroubleshootingActivity
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.interfaces.OnLicenseCheckResultListener
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.util.*
import com.xda.nobar.views.BarView
import kotlinx.android.synthetic.main.layout_main_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.layout_main_activity), OnGestureStateChangeListener,
    OnNavBarHideStateChangeListener, OnLicenseCheckResultListener, CoroutineScope by MainScope() {
    private val app: App
        get() = requireContext().app
    private val prefManager: PrefManager
        get() = requireContext().prefManager

    private val navListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        app.toggleNavState(!isChecked)
        if (!requireContext().hasWss) onNavStateChange(!isChecked)
    }

    private var currentPremReason: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!app.rootWrapper.isConnected) app.rootWrapper.onCreate()

        app.addLicenseCheckListener(this)
        app.addGestureActivationListener(this)
        app.addNavBarHideListener(this)

        activate.isChecked = prefManager.isActive
        activate.onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { button, isChecked ->
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(requireContext()))
                && requireContext().isAccessibilityEnabled) {
                if (isChecked) app.addBar() else app.removeBar()
                app.setGestureState(isChecked)
            } else {
                button.isChecked = false
                IntroActivity.start(requireContext())
            }
        }

        hide_nav.onCheckedChangeListener = navListener

        requireContext().checkNavHiddenAsync {
            onNavStateChange(it)
        }

        refresh_prem.setOnClickListener {
            refresh()
        }

        prem_stat_clicker.setOnClickListener {
            MaterialAlertDialogBuilder(requireActivity())
                .setMessage(currentPremReason)
                .show()
        }

        troubleshoot.setOnClickListener {
            startActivity(Intent(requireContext(), TroubleshootingActivity::class.java))
        }

        enable_left_side.isChecked = prefManager.leftSideGesture
        enable_left_side.onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            prefManager.leftSideGesture = isChecked
        }

        enable_right_side.isChecked = prefManager.rightSideGesture
        enable_right_side.onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            prefManager.rightSideGesture = isChecked
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()

        if (prefManager.hideBetaPrompt) {
            beta.visibility = View.GONE
        } else {
            beta.visibility = View.VISIBLE
            beta.setOnClickListener {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.sign_up_for_beta)
                    .setMessage(R.string.sign_up_for_beta_desc)
                    .setPositiveButton(android.R.string.ok) { _, _ -> context?.launchUrl("https://play.google.com/apps/testing/com.xda.nobar") }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.hide_text) { _,_ ->
                        findNavController().navigate(R.id.action_homeFragment_to_helpAboutActivity)
                    }
                    .show()
            }
        }
    }

    override fun onGestureStateChange(barView: BarView?, activated: Boolean) {
        activate.isChecked = activated
    }

    override fun onNavStateChange(hidden: Boolean) {
        hide_nav?.onCheckedChangeListener = null
        hide_nav?.isChecked = hidden
        hide_nav?.onCheckedChangeListener = navListener
    }

    override fun onResult(valid: Boolean, reason: String?) {
        currentPremReason = reason
        launch {
            prem_stat.setTextColor(if (valid) Color.GREEN else Color.RED)
            prem_stat.text = resources.getText(if (valid) R.string.installed else R.string.not_found)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()

        app.removeLicenseCheckListener(this)
        app.removeGestureActivationListener(this)
        app.removeNavBarHideListener(this)

        try {
            app.removeGestureActivationListener(this)
        } catch (e: Exception) {}
    }

    private fun refresh() {
        prem_stat.setTextColor(Color.YELLOW)
        prem_stat.text = resources.getText(R.string.checking)

        app.refreshPremium()
    }
}