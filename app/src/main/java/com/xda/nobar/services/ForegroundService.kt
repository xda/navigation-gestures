package com.xda.nobar.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.xda.nobar.R

/**
 * Used to prevent the device from killing NoBar
 */
class ForegroundService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            notificationManager.createNotificationChannel(NotificationChannel("nobar", resources.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = NotificationCompat.Builder(this, "nobar")
//                .setContentTitle(resources.getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_border_bottom_black_24dp)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(resources.getText(R.string.foreground_desc))
                        .setBigContentTitle(resources.getText(R.string.foreground)))

        startForeground(10, builder.build())

        return START_STICKY
    }
}
