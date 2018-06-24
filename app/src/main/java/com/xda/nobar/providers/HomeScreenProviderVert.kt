package com.xda.nobar.providers

import android.appwidget.AppWidgetManager
import android.content.Context
import com.xda.nobar.R

/**
 * Vertical homescreen widget provider
 */
class HomeScreenProviderVert : BaseProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val widgetViews = handleUpdate(context, R.layout.widget_layout_vertical)

        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, widgetViews)
        }
    }
}
