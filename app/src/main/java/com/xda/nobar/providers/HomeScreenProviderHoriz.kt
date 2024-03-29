package com.xda.nobar.providers

import android.appwidget.AppWidgetManager
import android.content.Context
import com.xda.nobar.R

/**
 * Horizontal homescreen widget provider
 */
class HomeScreenProviderHoriz : BaseProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val widgetViews = handleUpdate(context, R.layout.widget_layout_horiz)

        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, widgetViews)
        }
    }
}
