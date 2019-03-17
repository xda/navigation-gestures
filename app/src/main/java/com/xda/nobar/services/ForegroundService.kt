package com.xda.nobar.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xda.nobar.R
import com.xda.nobar.activities.MainActivity

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
                .setSmallIcon(R.drawable.ic_navgest)
                .setPriority(if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
                .setContentIntent(PendingIntent.getActivity(this, 10, MainActivity.makeIntent(this), 0))
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(resources.getText(R.string.foreground_desc))
                        .setBigContentTitle(resources.getText(R.string.foreground)))

        startForeground(10, builder.build())

        return START_STICKY
    }
}
