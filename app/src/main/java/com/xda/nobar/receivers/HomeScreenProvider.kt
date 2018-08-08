package com.xda.nobar.receivers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.MainActivity
import com.xda.nobar.providers.BaseProvider
import com.xda.nobar.providers.BaseProvider.Companion.ACTION_PERFORM_TOGGLE
import com.xda.nobar.providers.BaseProvider.Companion.ACTION_REFRESH
import com.xda.nobar.providers.BaseProvider.Companion.GEST
import com.xda.nobar.providers.BaseProvider.Companion.IMM
import com.xda.nobar.providers.BaseProvider.Companion.NAV
import com.xda.nobar.providers.BaseProvider.Companion.sendUpdate

class CocktailReceiver : BaseProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

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
                        this.onVisibilityChanged(context, cocktailId, visibility)
                    }
                }
            }
        } else {
            extras = intent.extras
            if (extras != null && extras.containsKey("cocktailIds")) {
                val cocktailIds = extras.getIntArray("cocktailIds")
                this.onUpdate(context, SlookCocktailManager.getInstance(context), cocktailIds)
            }
        }
        
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

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val widgetViews = handleUpdate(context, R.layout.widget_layout_horiz)

        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, widgetViews)
        }
    }

    override fun onUpdate(context: Context, cocktailManager: SlookCocktailManager, cocktailIds: IntArray) {
        val views = handleUpdate(context, R.layout.widget_layout)

        val longClickIntent = Intent(context, MainActivity::class.java)
        longClickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val longClickPendingIntent = PendingIntent.getActivity(context, 400, longClickIntent, 0)

        cocktailManager.setOnLongClickPendingIntent(views, R.id.root, longClickPendingIntent)

        for (id in cocktailIds) {
            cocktailManager.updateCocktail(id, views)
        }
    }
}
