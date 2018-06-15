package com.xda.nobar.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.xda.nobar.R

/**
 * Used for receiving notifications
 * THIS SHOULD NEVER BE USED!!
 */
class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            notificationManager.createNotificationChannel(NotificationChannel("nobar_notification", resources.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW))
        }

        val builder = NotificationCompat.Builder(this, "nobar_notification")
                .setSmallIcon(R.drawable.launcher_vector)
                .setContentTitle(msg?.notification?.title)
                .setContentText(msg?.notification?.body)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(msg?.notification?.body))

        notificationManager.notify(0, builder.build())
    }
}
