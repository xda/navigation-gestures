package com.xda.nobar.util

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException

object SuUtils {
    @JvmStatic
    fun sudo(vararg strings: String) {
        try {
            val su = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(su.outputStream)

            for (s in strings) {
                outputStream.writeBytes(s + "\n")
                outputStream.flush()
            }

            outputStream.writeBytes("exit\n")
            outputStream.flush()
            try {
                su.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
                Log.e("No Root?", e.message)
            }

            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun testSudo(): Boolean {
        return try {
            val su = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(su.outputStream)

            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val wait = su.waitFor()

            wait == 0
        } catch (e: Exception) {
            e.printStackTrace()

            false
        }
    }
}