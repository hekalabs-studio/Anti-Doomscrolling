package com.example.dumbscrolling.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.dumbscrolling.MainActivity
import com.example.dumbscrolling.R

class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Set initial state
        updateWidget(context, "Siap", "--:--")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_BUTTON) {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(mainIntent)
        }
    }

    companion object {
        const val ACTION_WIDGET_BUTTON = "com.example.dumbscrolling.ACTION_WIDGET_BUTTON"

        fun updateWidget(context: Context, status: String, time: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FocusWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_focus)
                views.setTextViewText(R.id.widgetStatus, status)
                views.setTextViewText(R.id.widgetTime, time)

                val intent = Intent(context, FocusWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_BUTTON
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                views.setOnClickPendingIntent(R.id.widgetActionBtn, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
