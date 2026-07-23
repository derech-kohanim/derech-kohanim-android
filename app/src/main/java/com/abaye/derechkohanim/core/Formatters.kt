package com.abaye.derechkohanim.core

import android.content.Context
import com.abaye.derechkohanim.R
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Distance, duration and arrival time in the interface language. Units live in the string
 * resources, so switching language switches them too (spec 14.1).
 */
object Formatters {

    fun distance(context: Context, meters: Double): String {
        val numbers = NumberFormat.getInstance()
        return if (meters < 1000.0) {
            val rounded = if (meters < 100) meters.roundToInt() else (meters / 10).roundToInt() * 10
            context.getString(R.string.unit_meters, numbers.format(rounded))
        } else {
            numbers.maximumFractionDigits = 1
            numbers.minimumFractionDigits = 1
            context.getString(R.string.unit_kilometers, numbers.format(meters / 1000.0))
        }
    }

    fun duration(context: Context, seconds: Double): String {
        val totalMinutes = (seconds / 60.0).roundToLong()
        if (totalMinutes < 1) return context.getString(R.string.duration_under_a_minute)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val numbers = NumberFormat.getInstance()
        return when {
            hours == 0L -> context.getString(R.string.duration_minutes, numbers.format(minutes))
            minutes == 0L -> context.getString(R.string.duration_hours, numbers.format(hours))
            else -> context.getString(
                R.string.duration_hours_minutes,
                numbers.format(hours),
                numbers.format(minutes),
            )
        }
    }

    fun speed(context: Context, metersPerSecond: Double): String {
        val kmh = (metersPerSecond * 3.6).roundToInt().coerceAtLeast(0)
        return context.getString(R.string.nav_speed_kmh, NumberFormat.getInstance().format(kmh))
    }

    fun arrivalTime(seconds: Double, now: Instant = Instant.now()): String {
        val arrival = now.plusSeconds(seconds.roundToLong())
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(arrival)
    }

    /**
     * Spec 17.4: the driver hears the cost of avoidance in minutes, never as a ratio.
     * The ratio stays in the debug surface.
     */
    fun avoidanceCost(context: Context, durationSeconds: Double, detourRatio: Double?): String? {
        if (detourRatio == null || detourRatio <= 1.02) return null
        val directSeconds = durationSeconds / detourRatio
        val extraMinutes = ((durationSeconds - directSeconds) / 60.0).roundToInt()
        if (extraMinutes < 1) return null
        return context.getString(
            R.string.route_avoidance_cost,
            NumberFormat.getInstance().format(extraMinutes),
        )
    }
}
