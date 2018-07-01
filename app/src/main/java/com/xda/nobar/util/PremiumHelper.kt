package com.xda.nobar.util

import android.Manifest
import android.accounts.AccountManager
import android.content.*
import android.content.pm.PackageManager
import com.xda.nobar.interfaces.OnLicenseCheckResultListener

/**
 * Helper class for managing premium detection
 */
class PremiumHelper(private val context: Context, private val listener: OnLicenseCheckResultListener) {
    companion object {
        const val COMPANION_PACKAGE = "com.xda.nobar.premium"

        val BYPASS = arrayListOf(
                "zachary.wander@gmail.com",
                "mishaal@xda-developers.com",
                "mmrahman123@gmail.com"
        )
    }

    val isCompanionInstalled: Boolean
        get() {
            return try {
                context.packageManager.getApplicationInfo(COMPANION_PACKAGE, 0).enabled
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.xda.nobar.action.PREMIUM_RESULT") {
                listener.onResult(intent.getBooleanExtra("valid", false), intent.getStringExtra("msg"))
            }
        }
    }

    fun checkPremium() {
        if (context.checkCallingOrSelfPermission(Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            val accounts = AccountManager.get(context).accounts

            accounts.forEach {
                if (BYPASS.contains(it.name)) {
                    listener.onResult(true, "Bypass email: ${it.name}")
                    return
                }
            }

        }

        checkInternal()
    }

    init {
        val filter = IntentFilter("com.xda.nobar.action.PREMIUM_RESULT")
        context.registerReceiver(receiver, filter, "com.xda.nobar.permission.VERIFY_LICENSE", null)
    }

    private fun checkInternal() {
        if (!isCompanionInstalled) listener.onResult(false, "Premium Add-On not installed")
        else {
            val intent = Intent("com.xda.nobar.action.PREMIUM_CHECK")
            intent.component = ComponentName("com.xda.nobar.premium", "com.xda.nobar.premium.Receiver")
            context.sendBroadcast(intent)
        }
    }
}