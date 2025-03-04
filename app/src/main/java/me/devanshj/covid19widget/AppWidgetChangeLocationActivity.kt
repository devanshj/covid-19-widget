package me.devanshj.covid19widget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.os.Bundle

class AppWidgetChangeLocationActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidget = AppWidget.from(
            intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
            this
        )
        val state = appWidget.getState()!!

        AlertDialog.Builder(this)
            .setTitle("Select Location")
            .setSingleChoiceItems(
                locations.map { l -> l.name }.toTypedArray(),
                locations.indexOfFirst { l -> l.identifier === state.location.identifier }
            ) { _, selectedIndex ->
                appWidget.setState(AppWidgetState(
                    state.version,
                    locations.elementAt(selectedIndex),
                    state.status,
                    state.graph
                ))
                finish()
            }
            .setOnCancelListener { finish() }
            .create()
            .show()
    }
}