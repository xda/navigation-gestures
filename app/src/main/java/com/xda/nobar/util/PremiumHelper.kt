package com.xda.nobar.util

import android.Manifest
import android.accounts.AccountManager
import android.content.*
import android.content.pm.PackageManager
import android.util.Patterns
import com.github.javiersantos.piracychecker.PiracyChecker
import com.github.javiersantos.piracychecker.enums.InstallerID
import com.github.javiersantos.piracychecker.enums.PiracyCheckerCallback
import com.github.javiersantos.piracychecker.enums.PiracyCheckerError
import com.github.javiersantos.piracychecker.enums.PirateApp
import com.xda.nobar.interfaces.OnLicenseCheckResultListener

/**
 * Helper class for managing premium detection
 */
class PremiumHelper(private val context: Context, private val listener: OnLicenseCheckResultListener) {
    companion object {
        const val COMPANION_PACKAGE = "com.xda.nobar.premium"

        private val BYPASS = arrayListOf(
                "zachary.wander@gmail.com",
                "mishaal@xda-developers.com",
                "mmrahman123@gmail.com"
        )
    }

    private val isCompanionInstalled: Boolean
        get() {
            return try {
                context.packageManager.getPackageInfo(COMPANION_PACKAGE, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

    private val checker = PiracyChecker(context.applicationContext)
            .enableSigningCertificate("XbHBiv0+y/+w8q0KNdKP/6EQT54=")
            .enableGooglePlayLicensing("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzXiFD3XTGpPsUCcb6ipshaHZ0U6VKeW8oxTKPH4p90lqvumLqBZA4EACg+icC9aeHPpPNA9Qd3X+h8KScQWvZoCbgXF7HWJnCMrBBxTrK7VGIIRzSvrQvV/ESyjVj7f7HhkfDFCqZy/AQqqAXLtSTQUU3mVcjEuggaYgKODwZlQFm12yb+aNuG6vZvR9B8onK8lzaJNgLATbTh165VjnWDy5Xcu8IJgNB1wRynvfPXVoo+jPgiIXyIC3s3KzzA9ySPxdfZ4DCiWDecDaLLxZN7WYhJruCYn5Ph/rxE9+cl65zBsoujQS7c7nX4CbTLSEkt6My+3m/S9sNzbMjcvVIwIDAQAB")
            .enableInstallerId(InstallerID.GOOGLE_PLAY)
            .callback(object : PiracyCheckerCallback() {
                override fun dontAllow(error: PiracyCheckerError, app: PirateApp?) {
                    listener.onResult(false, error.toString())
                }

                override fun onError(error: PiracyCheckerError) {
                    listener.onResult(false, error.toString())
                }

                override fun allow() {
                    listener.onResult(true, "PiracyChecker valid")
                }
            })

    fun checkPremium() {
        if (!isCompanionInstalled) listener.onResult(false, "Premium Add-On not installed")
        else {
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
    }

    private fun checkInternal() {
        checker.start()
    }
}