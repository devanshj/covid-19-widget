package me.devanshj.covid19widget

import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


fun fetchRecords(state: AppWidgetState) =
    { l: List<Pair<Date, Int>> ->
        when (state.graph.type) {
            AppWidgetState.Graph.Type.DAILY -> l
            AppWidgetState.Graph.Type.CUMULATIVE -> cumulative(l)
        }
    }.let { transform ->
        fetchDailyStats(state.location).let { (statuses, forStatus) ->
            Pair(
                transform(forStatus(state.status)),
                statuses
                    .fold(emptyList<Pair<Date, Int>>()) { stats, s -> stats.plus(transform(s)) }
                    .map { (_, c) -> c }
                    .max()!!
            )
        }
    }

fun cumulative(counts: List<Pair<Date, Int>>) =
    (0 .. counts.lastIndex).map { i ->
        Pair(
            counts.elementAt(i).first,
            counts.take(i + 1).sumBy { (_, c) -> c }
        )
    }

fun fetchDailyStats(location: AppWidgetState.Location): Pair<List<List<Pair<Date, Int>>>, (AppWidgetState.Status) -> List<Pair<Date, Int>>> {
    val rows =
        URL("https://api.covid19india.org/states_daily.json").readText()
            .let { raw -> JSONObject(raw) }
            .getJSONArray("states_daily")
            .let { arr -> List(arr.length()) { i -> arr.getJSONObject(i) } }

    val forStatus = { s: AppWidgetState.Status ->
        rows
            .filter { row ->
                row.getString("status") == when (s) {
                    AppWidgetState.Status.CONFIRMED -> "Confirmed"
                    AppWidgetState.Status.ACTIVE -> "Active"
                    AppWidgetState.Status.DECEASED -> "Deceased"
                    AppWidgetState.Status.RECOVERED -> "Recovered"
                }
            }
            .map { row ->
                Pair(
                    row
                        .getString("date")
                        .let { s -> SimpleDateFormat("dd-MMM-yy").parse(s) },

                    row
                        .getString(
                            if (location.identifier == "IN") "tt"
                            else location.identifier.replace("IN-", "").toLowerCase(Locale.ROOT)
                        )
                        .toInt()
                )
            }
            .sortedBy { (date, _) -> date }
    }

    val confirmed = forStatus(AppWidgetState.Status.CONFIRMED)
    val recovered = forStatus(AppWidgetState.Status.RECOVERED)
    val deceased = forStatus(AppWidgetState.Status.DECEASED)
    val active =
        (0 .. confirmed.lastIndex).map { i ->
            Pair(
                confirmed[i].first,
                confirmed[i].second -
                        recovered.getOrElse(i) { Pair(Date(), 0) }.second -
                        deceased.getOrElse(i) { Pair(Date(), 0) }.second
            )
        }

    return Pair(
        listOf(confirmed, active, recovered, deceased),
        { s: AppWidgetState.Status ->
            when(s) {
                AppWidgetState.Status.CONFIRMED -> confirmed
                AppWidgetState.Status.ACTIVE -> active
                AppWidgetState.Status.RECOVERED -> recovered
                AppWidgetState.Status.DECEASED -> deceased
            }
        }
    )
}

fun fetchCountAndDelta(location: AppWidgetState.Location, status: AppWidgetState.Status): Pair<Int, Int> =
    URL("https://api.covid19india.org/data.json").readText()
        .let { raw -> JSONObject(raw) }
        .getJSONArray("statewise")
        .let { arr -> List(arr.length()) { i -> arr.getJSONObject(i) } }
        .find { row ->
            row.getString("statecode") == when(location.identifier) {
                "IN" -> "TT"
                else -> location.identifier.replace("IN-", "")
            }
        }!!
        .let { row ->
            val statusProp = when(status){
                AppWidgetState.Status.CONFIRMED -> "confirmed"
                AppWidgetState.Status.ACTIVE -> "active"
                AppWidgetState.Status.RECOVERED -> "recovered"
                AppWidgetState.Status.DECEASED -> "deaths"
            }

            return Pair(
                row.getString(statusProp).toInt(),
                when (status) {
                    AppWidgetState.Status.ACTIVE -> 0
                    else -> row.getString("delta" + statusProp).toInt()
                }
            )
        }