package com.xda.nobar.util

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException

/**
 * Helper for performing root-related actions
 */
object SuUtils {
    /**
     * Run commands as root
     * @param strings series of commands
     */
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

    /**
     * Test for sudo presence and permission
     * @return true if root is present and granted to NoBar
     */
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

    /**
     * Get a su process
     * @return a new su process
     */
    fun getSudo(): Process? {
        return try {
            val su = Runtime.getRuntime().exec("su")

            if (su.waitFor() == 0) su
            else null
        } catch (e: Exception) {
            null
        }
    }
}