@file:Suppress("DEPRECATION")

package com.xda.nobar.providers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.SettingsActivity
import dalvik.system.DexFile
import java.io.IOException

/**
 * Base provider for all the widgets
 * Handles the basic logic: updating states, listening for touches, etc
 */
abstract class BaseProvider: AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "com.xda.nobar.action.REFRESH_STATE"
        const val ACTION_PERFORM_TOGGLE = "com.xda.nobar.action.PERFORM_TOGGLE"

        const val EXTRA_WHICH = "which"

        const val NAV = 0
        const val GEST = 1
        const val IMM = 2

        fun sendUpdate(context: Context) {
            try {
                val packageCodePath = context.packageCodePath
                val df = DexFile(packageCodePath)
                val iter = df.entries()
                while (iter.hasMoreElements()) {
                    val className = iter.nextElement()

                    if (className.contains("${context.applicationContext.packageName}.providers")) {
                        val clazz = Class.forName(className)

                        if (clazz.superclass == BaseProvider::class.java && clazz != BaseProvider::class.java) {
                            val intent = Intent(context.applicationContext, clazz)
                            intent.action = BaseProvider.ACTION_REFRESH
                            context.applicationContext.sendBroadcast(intent)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
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
            ACTION_REFRESH -> {
                val manager = AppWidgetManager.getInstance(context)
                onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, javaClass)))
            }
        }

        val action = intent.action
        val extras: Bundle?
        if ("com.samsung.android.cocktail.action.COCKTAIL_UPDATE" != action && "com.samsung.android.cocktail.v2.action.COCKTAIL_UPDATE" != action) {
            if ("com.samsung.android.cocktail.action.COCKTAIL_ENABLED" == action) {
                this.onEnabled(context)
            } else if ("com.samsung.android.cocktail.action.COCKTAIL_DISABLED" == action) {
                this.onDisabled(context)
            } else if ("com.samsung.android.cocktail.action.COCKTAIL_VISIBILITY_CHANGED" == action) {
                extras = intent.extras
                if (extras != null && extras.containsKey("cocktailId")) {
                    val cocktailId = extras.getInt("cocktailId")
                    if (extras.containsKey("cocktailVisibility")) {
                        val visibility = extras.getInt("cocktailVisibility")
                        onVisibilityChanged(context, cocktailId, visibility)
                    }
                }
            }
        } else {
            extras = intent.extras
            if (extras != null && extras.containsKey("cocktailIds")) {
                val cocktailIds = extras.getIntArray("cocktailIds") ?: return
                onUpdate(context, SlookCocktailManager.getInstance(context), cocktailIds)
            }
        }
    }

    internal fun handleUpdate(context: Context, @LayoutRes layout: Int): RemoteViews {
        val app = context.applicationContext as App
        val views = RemoteViews(context.packageName, layout)

        val gestures = app.prefManager.isActive
        val hideNav = app.prefManager.shouldUseOverscanMethod
        val useImm = app.prefManager.useImmersiveWhenNavHidden

        views.setTextViewText(R.id.gesture_status, context.resources.getText(
                if (gestures) R.string.gestures_on else R.string.gestures_off))
        views.setTextViewText(R.id.nav_status, context.resources.getText(
                if (hideNav) R.string.nav_hidden else R.string.nav_shown))
        views.setTextViewText(R.id.imm_status, context.resources.getText(
                if (useImm) R.string.nav_imm_enabled else R.string.nav_imm_disabled))

        views.setInt(R.id.toggle_gestures, "setColorFilter",
                ContextCompat.getColor(context, if (gestures) R.color.colorAccent else R.color.color_disabled))
        views.setInt(R.id.toggle_nav, "setColorFilter",
                ContextCompat.getColor(context, if (hideNav) R.color.colorAccent else R.color.color_disabled))
        views.setInt(R.id.toggle_imm, "setColorFilter",
                ContextCompat.getColor(context, if (useImm) R.color.colorAccent else R.color.color_disabled))

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

        return views
    }

    internal open fun onVisibilityChanged(context: Context, cocktailId: Int, visibility: Int) {}

    internal open fun onUpdate(context: Context, manager: SlookCocktailManager, ids: IntArray) {}
}