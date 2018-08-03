package com.xda.nobar.providers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.xda.nobar.R
import com.xda.nobar.activities.MainActivity

/**
 * Widget provider for TouchWiz's Edge Screen framework
 */
class CocktailProvider : BaseProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        val manager = SlookCocktailManager.getInstance(context)
        val ids = manager.getCocktailIds(ComponentName(context, javaClass))
        onUpdate(context, manager, ids)
    }

    override fun onUpdate(context: Context, manager: SlookCocktailManager, ids: IntArray) {
        val views = handleUpdate(context, R.layout.widget_layout)
        val longClickPendingIntent = PendingIntent.getActivity(context, 400, MainActivity.makeIntent(context), 0)

        manager.setOnLongClickPendingIntent(views, R.id.root, longClickPendingIntent)

        for (id in ids) {
            manager.updateCocktail(id, views)
        }
    }
}