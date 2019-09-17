package com.xda.nobar.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xda.nobar.R

class KeepAliveService : Service() {
    companion object {
        private const val KEEP_ALIVE_CHANNEL = "keep_alive"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        val builder = NotificationCompat.Builder(this, KEEP_ALIVE_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(resources.getText(R.string.keep_alive))
                .setContentText(resources.getText(R.string.keep_alive_service_text))
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(resources.getText(R.string.keep_alive_service_text)))
                .setPriority(NotificationCompat.PRIORITY_LOW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    KEEP_ALIVE_CHANNEL,
                    resources.getText(R.string.keep_alive),
                    NotificationManager.IMPORTANCE_LOW
            )

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        startForeground(100, builder.build())
    }
}