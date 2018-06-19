package com.xda.nobar.receivers

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.MainActivity
import com.xda.nobar.activities.SettingsActivity

class CocktailReceiver : SlookCocktailProvider() {
    companion object {
        const val ACTION_REFRESH = "com.xda.nobar.action.REFRESH_STATE"
        const val ACTION_PERFORM_TOGGLE = "com.xda.nobar.action.PERFORM_TOGGLE"
        
        const val EXTRA_WHICH = "which"
        
        const val NAV = 0
        const val GEST = 1
        const val IMM = 2

        fun sendUpdate(context: Context) {
            val intent = Intent(context, CocktailReceiver::class.java)
            intent.action = ACTION_REFRESH
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH -> {
                val manager = SlookCocktailManager.getInstance(context)
                val ids = manager.getCocktailIds(ComponentName(context, javaClass))
                onUpdate(context, manager, ids)
            }
            ACTION_PERFORM_TOGGLE -> {
                if (intent.hasExtra(EXTRA_WHICH)) {
                    val app = context.applicationContext as App
                    val which = intent.getIntExtra(EXTRA_WHICH, -1)
                    when (which) {
                        GEST -> {
                            app.toggleGestureBar()
                            sendUpdate(context)
                        }
                        NAV -> {
                            app.toggleNavState()
                            sendUpdate(context)
                        }
                        IMM -> {
                            app.toggleImmersiveWhenNavHidden()
                            sendUpdate(context)
                        }
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, cocktailManager: SlookCocktailManager, cocktailIds: IntArray) {
        val app = context.applicationContext as App

        val gestures = app.areGesturesActivated()
        val hideNav = app.prefs.getBoolean("hide_nav", false)
        val useImm = app.prefs.getBoolean("use_immersive_mode_when_nav_hidden", false)

        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.gesture_status, context.resources.getText(
                if (gestures) R.string.gestures_on else R.string.gestures_off))
        views.setTextViewText(R.id.nav_status, context.resources.getText(
                if (hideNav) R.string.nav_hidden else R.string.nav_shown))
        views.setTextViewText(R.id.imm_status, context.resources.getText(
                if (useImm) R.string.nav_imm_enabled else R.string.nav_imm_disabled))

        views.setInt(R.id.toggle_gestures, "setColorFilter",
                context.resources.getColor(if (gestures) R.color.colorAccent else R.color.color_disabled))
        views.setInt(R.id.toggle_nav, "setColorFilter",
                context.resources.getColor(if (hideNav) R.color.colorAccent else R.color.color_disabled))
        views.setInt(R.id.toggle_imm, "setColorFilter",
                context.resources.getColor(if (useImm) R.color.colorAccent else R.color.color_disabled))

        val toggleGestureIntent = Intent(context, javaClass)
        toggleGestureIntent.action = ACTION_PERFORM_TOGGLE
        toggleGestureIntent.putExtra(EXTRA_WHICH, GEST)

        val toggleNavIntent = Intent(context, javaClass)
        toggleNavIntent.action = ACTION_PERFORM_TOGGLE
        toggleNavIntent.putExtra(EXTRA_WHICH, NAV)

        val useImmIntent = Intent(context, javaClass)
        useImmIntent.action = ACTION_PERFORM_TOGGLE
        useImmIntent.putExtra(EXTRA_WHICH, IMM)

        val settingsIntent = Intent(context, SettingsActivity::class.java)
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        views.setOnClickPendingIntent(R.id.toggle_gestures, PendingIntent.getBroadcast(context, GEST, toggleGestureIntent, 0))
        views.setOnClickPendingIntent(R.id.toggle_nav, PendingIntent.getBroadcast(context, NAV, toggleNavIntent, 0))
        views.setOnClickPendingIntent(R.id.toggle_imm, PendingIntent.getBroadcast(context, IMM, useImmIntent, 0))
        views.setOnClickPendingIntent(R.id.settings, PendingIntent.getActivity(context, 401, settingsIntent, 0))

        val longClickIntent = Intent(context, MainActivity::class.java)
        longClickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val longClickPendingIntent = PendingIntent.getActivity(context, 400, longClickIntent, 0)

        cocktailManager.setOnLongClickPendingIntent(views, R.id.root, longClickPendingIntent)

        for (id in cocktailIds) {
            cocktailManager.updateCocktail(id, views)
        }
    }
}
