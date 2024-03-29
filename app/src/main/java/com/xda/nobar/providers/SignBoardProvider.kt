package com.xda.nobar.providers

import android.appwidget.AppWidgetManager
import android.content.Context
import com.xda.nobar.R

/**
 * Provider for the LG V10/V20 SignBoard
 */
class SignBoardProvider : BaseProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val widgetViews = handleUpdate(context, R.layout.widget_layout_signboard)

        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, widgetViews)
        }
    }
}