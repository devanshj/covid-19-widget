package me.devanshj.covid19widget

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt


class AppWidgetState(
    val version: Version,
    val location: Location,
    val status: Status,
    val graph: Graph
) {

    fun encode() =
        encodeList(
            version.encode(),
            location.encode(),
            status.encode(),
            graph.encode()
        )

    fun reduce(action: AppWidgetAction) =
        AppWidgetState(
            version.reduce(action),
            location.reduce(action),
            status.reduce(action),
            graph.reduce(action)
        )

    companion object {
        fun fromEncoded(s: String) =
            decodeList(s).let { a ->
                AppWidgetState(
                    Version.fromEncoded(a[0]),
                    Location.fromEncoded(a[1]),
                    Status.fromEncoded(a[2]),
                    Graph.fromEncoded(a[3])
                )
            }

        val initialState = AppWidgetState(
            version = Version.ONE,
            location = locations[0],
            status = Status.CONFIRMED,
            graph = Graph(
                type = Graph.Type.DAILY,
                timeSeries = Graph.TimeSeries.TWENTY_DAYS,
                scaleType = Graph.ScaleType.LINEAR
            )
        )
    }


    sealed class Version(protected val identifier: String) {
        fun encode() =
            encodeList(identifier)

        fun reduce(action: AppWidgetAction) =
            this

        companion object {
            fun fromEncoded(s: String) =
                when (decodeList(s)[0]) {
                    ONE.identifier -> ONE
                    else -> null
                }!!
        }


        object ONE: Version("1")
    }

    class Location(
        val identifier: String,
        val name: String
    ) {
        val shortText = identifier

        fun encode() =
            encodeList(identifier)

        fun reduce(action: AppWidgetAction) =
            this

        companion object {
            fun fromEncoded(s: String) =
                decodeList(s)[0]
                .let { identifier -> locations.find { l -> l.identifier == identifier }!! }

        }
    }

    sealed class Status(val identifier: String, @ColorInt val color: Int, val viewText: String) {
        fun encode() =
            encodeList(identifier)

        fun reduce(action: AppWidgetAction) =
            when(action) {
                AppWidgetAction.ON_STATUS_CLICK -> when (this) {
                    CONFIRMED -> ACTIVE
                    ACTIVE -> RECOVERED
                    RECOVERED -> DECEASED
                    DECEASED -> CONFIRMED
                }
                else -> this
            }

        companion object {
            fun fromEncoded(s: String) =
                when (decodeList(s)[0]) {
                    CONFIRMED.identifier -> CONFIRMED
                    ACTIVE.identifier -> ACTIVE
                    RECOVERED.identifier -> RECOVERED
                    DECEASED.identifier -> DECEASED
                    else -> null
                }!!
        }

        object CONFIRMED: Status("CONFIRMED", Color.parseColor("#FF073A"), "CONFIRMED")
        object ACTIVE: Status("ACTIVE", Color.parseColor("#007BFF"), "ACTIVE")
        object RECOVERED: Status("RECOVERED", Color.parseColor("#28A745"), "RECOVERED")
        object DECEASED: Status("DECEASED", Color.parseColor("#6C757D"), "DECEASED")
    }

    class Graph(val type: Type, val timeSeries: TimeSeries, val scaleType: ScaleType) {

        fun encode() =
            encodeList(type.encode(), timeSeries.encode(), scaleType.encode())

        fun reduce(action: AppWidgetAction) =
            Graph(
                this.type.reduce(action),
                this.timeSeries.reduce(action),
                this.scaleType.reduce(action)
            )

        companion object {
            fun fromEncoded(s: String) =
                decodeList(s).let { a ->
                    Graph(
                        Type.fromEncoded(a[0]),
                        TimeSeries.fromEncoded(a[1]),
                        ScaleType.fromEncoded(a[2])
                    )
                }
        }

        sealed class Type(protected val identifier: Int, val viewText: String) {
            fun encode() =
                encodeList(identifier.toString())

            fun reduce(action: AppWidgetAction) =
                when(action) {
                    AppWidgetAction.ON_GRAPH_TYPE_CLICK -> when (this) {
                        DAILY -> CUMULATIVE
                        CUMULATIVE -> DAILY
                    }
                    else -> this
                }

            companion object {
                fun fromEncoded(s: String) =
                    when (decodeList(s)[0].toInt()) {
                        DAILY.identifier -> DAILY
                        CUMULATIVE.identifier -> CUMULATIVE
                        else -> null
                    }!!
            }


            object DAILY: Type(1 shl 0, "DLY")
            object CUMULATIVE: Type(1 shl 1, "CUM")
        }

        sealed class TimeSeries(protected val identifier: Int, val viewText: String) {
            fun encode() =
                encodeList(identifier.toString())

            fun reduce(action: AppWidgetAction) =
                when(action) {
                    AppWidgetAction.ON_GRAPH_TIME_SERIES_CLICK -> when (this) {
                        TEN_DAYS -> TWENTY_DAYS
                        TWENTY_DAYS -> ONE_MONTH
                        ONE_MONTH -> BEGINNING
                        BEGINNING -> TEN_DAYS
                    }
                    else -> this
                }

            companion object {
                fun fromEncoded(s: String) =
                    when (decodeList(s)[0].toInt()) {
                        TEN_DAYS.identifier -> TEN_DAYS
                        TWENTY_DAYS.identifier -> TWENTY_DAYS
                        ONE_MONTH.identifier -> ONE_MONTH
                        BEGINNING.identifier -> BEGINNING
                        else -> null
                    }!!
            }


            object TEN_DAYS: TimeSeries(1 shl 0, "10D")
            object TWENTY_DAYS: TimeSeries(1 shl 1, "20D")
            object ONE_MONTH: TimeSeries(1 shl 2, "1M")
            object BEGINNING: TimeSeries(1 shl 3, "BEG")
        }

        sealed class ScaleType(protected val identifier: Int, val viewText: String) {
            fun encode() =
                encodeList(identifier.toString())

            companion object {
                fun fromEncoded(s: String) =
                    when (decodeList(s)[0].toInt()) {
                        LINEAR.identifier -> LINEAR
                        LOGARITHMIC.identifier -> LOGARITHMIC
                        else -> null
                    }!!
            }

            fun reduce(action: AppWidgetAction) =
                when(action) {
                    AppWidgetAction.ON_GRAPH_SCALE_TYPE_CLICK -> when (this) {
                        LINEAR -> LOGARITHMIC
                        LOGARITHMIC -> LINEAR
                    }
                    else -> this
                }

            object LINEAR: ScaleType(1 shl 0, "LIN")
            object LOGARITHMIC: ScaleType(1 shl 1, "LOG")
        }
    }
}

fun encodeList(vararg a: String) =
    "(" + listOf(*a).joinToString(",") + ")"

fun decodeList(encoded: String): List<String> =
    encoded
    .replace(Regex("^\\("), "")
    .replace(Regex("\\)$"), "")
    .split(",")
    .let { _chunks ->
        var chunks = _chunks
        var decoded = emptyList<String>()
        while (chunks.isNotEmpty()) {
            val chunk = chunks.elementAt(0)
            val isAtom =
                chunk
                    .split("")
                    .map { char ->
                        when(char) {
                            "(" -> 1
                            ")" -> -1
                            else -> 0
                        }
                    }
                    .sum() == 0

            if (isAtom) {
                decoded = decoded.plus(chunk)
                chunks = chunks.drop(1)
            } else {
                chunks = listOf(chunk + "," + chunks.elementAt(1)).plus(chunks.drop(2))
            }
        }
        return decoded
    }



