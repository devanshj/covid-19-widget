package me.devanshj.covid19widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class AppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            val widget = AppWidget.from(appWidgetId, context)
            val state = widget.getState()

            if (state !== null) widget.render()
            else widget.setState(AppWidgetState.initialState)
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        Log.d("onReceive", intent.action.orEmpty())
        if (intent.action.equals("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS")) {
            AppWidget.from(
                intent.extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID),
                context
            ).render()
        }

        if (!intent.action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) return

        val action = intent.getStringExtra(EXTRA_APP_WIDGET_ACTION)
        if (action !== null) {
            Log.d("AppWidgetProvider.onReceive", action)

            AppWidget
            .from(
                intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                ),
                context
            )
            .onAction(AppWidgetAction.valueOf(action))
        } else {
            super.onReceive(context, intent)
        }
    }
}
