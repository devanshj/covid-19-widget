package me.devanshj.covid19widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.*
import java.lang.Exception
import java.text.NumberFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random


class AppWidget private constructor(val appWidgetId: Int, val context: Context) {

    fun getState() =
        context
        .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        .getString(PREFERENCE_STATE_KEY + appWidgetId, null)
        ?.let { s -> AppWidgetState.fromEncoded(s) }

    fun setState(value: AppWidgetState) {
        context
        .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREFERENCE_STATE_KEY + appWidgetId, value.encode())
        .apply()

        render()
    }

    fun isSmall() =
        AppWidgetManager.getInstance(context)
        .getAppWidgetOptions(appWidgetId)
        .let { o -> Pair(
            o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
            o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        ) }
        .let { (width, height) ->
            abs(width - 110) < abs(width - 180) ||
            abs(height - 40) < abs(height - 110)
        }

    fun render() {
        val widget = this
        Log.d("render", "isSmall = ${isSmall()}")
        GlobalScope.launch {
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, rootView(widget))
        }
    }

    fun onAction(action: AppWidgetAction) {
        this.setState(this.getState()!!.reduce(action))
    }

    companion object {
        fun from(appWidgetId: Int, context: Context) =
            AppWidget(appWidgetId, context)

        private const val PREFERENCE_NAME = "me.devanshj.covid19widget.AppWidget"
        private const val PREFERENCE_STATE_KEY = "state_"
    }
}


suspend fun rootView(widget: AppWidget): RemoteViews {

    val state = widget.getState()!!
    val context = widget.context
    val appWidgetId = widget.appWidgetId
    val isSmall = widget.isSmall()
    val views = RemoteViews(
        context.packageName,
        if (isSmall) R.layout.app_widget_small
        else R.layout.app_widget
    )

    withContext(Dispatchers.Default) {

        val (count, delta) = withContext(Dispatchers.IO) {
            fetchCountAndDelta(
                state.location,
                state.status
            )
        }

        listOf(
            Triple(
                R.id.widgetStatusTextView,
                if (!isSmall) state.status.viewText + ","
                else state.status.viewText.elementAt(0) + ",",
                pendingBroadcastWithAction(AppWidgetAction.ON_STATUS_CLICK, appWidgetId, context)
            ),
            Triple(
                R.id.widgetLocationTextView,
                state.location.shortText,
                PendingIntent.getActivity(
                    context.applicationContext,
                    Random.nextInt(),
                    Intent(context, AppWidgetChangeLocationActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                    0
                )
            ),
            Triple(
                R.id.widgetCountTextView,
                NumberFormat.getInstance().format(count),
                null
            ),
            Triple(
                R.id.widgetDeltaCountTextView,
                if (delta == 0) "" else "(+${NumberFormat.getInstance().format(delta)})",
                null
            )
        )
        .plus(if (isSmall) emptyList() else listOf(
            Triple(
                R.id.widgetGraphTypeTextView,
                state.graph.type.viewText + ",",
                pendingBroadcastWithAction(
                    AppWidgetAction.ON_GRAPH_TYPE_CLICK,
                    appWidgetId,
                    context
                )
            ),
            Triple(
                R.id.widgetGraphScaleTextView,
                state.graph.scaleType.viewText + ",",
                pendingBroadcastWithAction(
                    AppWidgetAction.ON_GRAPH_SCALE_TYPE_CLICK,
                    appWidgetId,
                    context
                )
            ),
            Triple(
                R.id.widgetGraphTimeSeriesTextView,
                state.graph.timeSeries.viewText,
                pendingBroadcastWithAction(
                    AppWidgetAction.ON_GRAPH_TIME_SERIES_CLICK,
                    appWidgetId,
                    context
                )
            )
        ))
        .forEach { item ->
            val (id, text, intent) = item
            views.setTextViewText(id, text)
            views.setTextColor(id, state.status.color)
            if (intent !== null) views.setOnClickPendingIntent(id, intent)
        }
    }

    if (!isSmall) {
        withContext(Dispatchers.Default) {
            views.setImageViewBitmap(
                R.id.widgetGraphImageView,
                graph(state, context.resources.displayMetrics.density)
            )
        }
    }

    return views
}


fun pendingBroadcastWithAction(action: AppWidgetAction, appWidgetId: Int, context: Context) =
    PendingIntent.getBroadcast(
        context,
        Random.nextInt(),
        Intent(context, AppWidgetProvider::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(EXTRA_APP_WIDGET_ACTION, action.toString()),
        0
    )!!



const val GRAPH_WIDTH = 160f
const val GRAPH_HEIGHT = 35f
const val STROKE_THICKNESS = 2f

suspend fun graph(state: AppWidgetState, dpi: Float): Bitmap {
    val records = withContext(Dispatchers.IO) { fetchRecords(state) }
    return Bitmap.createBitmap(
        (GRAPH_WIDTH * dpi).toInt(),
        (GRAPH_HEIGHT * dpi).toInt(),
        Bitmap.Config.ARGB_8888
    ).also { bitmap ->
        Canvas(bitmap).also { canvas ->
            val counts =
                records
                    .first
                    .filter { (date, _) -> date.before(Date()) }
                    .map { (_, count) -> count }
                    .takeLast(when(state.graph.timeSeries) {
                        AppWidgetState.Graph.TimeSeries.TEN_DAYS -> 10
                        AppWidgetState.Graph.TimeSeries.TWENTY_DAYS -> 20
                        AppWidgetState.Graph.TimeSeries.ONE_MONTH -> 30
                        AppWidgetState.Graph.TimeSeries.BEGINNING -> Int.MAX_VALUE
                    })


            val unscaledMaxY = records.second.toDouble()
            val base = exp(ln(unscaledMaxY) / (GRAPH_HEIGHT - STROKE_THICKNESS * 3))
            val yScale = { y: Double ->
                when (state.graph.scaleType) {
                    AppWidgetState.Graph.ScaleType.LINEAR ->
                        y * ((GRAPH_HEIGHT - STROKE_THICKNESS * 3) / unscaledMaxY)
                    AppWidgetState.Graph.ScaleType.LOGARITHMIC ->
                        log(y, base).let { y -> if (y.isFinite()) y else 0.0 }
                }
            }

            canvas.drawPath(
                cardinalCurve(
                    (0 .. counts.lastIndex).map { i ->
                        PointF(
                            i.toFloat()
                                .let { x -> x * ((GRAPH_WIDTH - STROKE_THICKNESS * 3) / counts.lastIndex) }
                                .let { x -> x + STROKE_THICKNESS }
                                .let { x -> x * dpi },
                            counts[i].toDouble()
                                .let { y -> yScale(y) }
                                .let { y -> ((GRAPH_HEIGHT - STROKE_THICKNESS * 3) - y) }
                                .let { y -> y + STROKE_THICKNESS }
                                .let { y -> y * dpi }
                                .toFloat()
                        )
                    }
                ),
                Paint().also {
                    it.style = Paint.Style.STROKE
                    it.strokeCap = Paint.Cap.ROUND
                    it.strokeWidth = STROKE_THICKNESS * dpi
                    it.isAntiAlias = true
                    it.color = state.status.color
                    it.alpha = (255 * 0.6f).toInt()
                }
            )
        }
    }
}