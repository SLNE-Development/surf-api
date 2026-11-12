package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import java.time.temporal.Temporal
import kotlin.time.Duration

/**
 * This timestamp plus [duration], computed by the database (PostgreSQL or MariaDB).
 *
 * Works on every timestamp column of the library and of Exposed's java-time module, and keeps the column type of
 * this expression. The part of [duration] below one microsecond is dropped.
 *
 * @throws IllegalArgumentException if [duration] is infinite or this is a date column; shift dates with [plusDays]
 */
operator fun <T : Temporal?> ExpressionWithColumnType<T>.plus(duration: Duration): ExpressionWithColumnType<T> {
    require(duration.isFinite()) { "duration must be finite, was $duration" }
    return TemporalShiftOp(this, duration.inWholeMicroseconds, TemporalShiftOp.ShiftUnit.MICROSECONDS)
}

/**
 * This timestamp minus [duration], computed by the database (PostgreSQL or MariaDB).
 *
 * @throws IllegalArgumentException if [duration] is infinite or this is a date column; shift dates with [minusDays]
 * @see plus
 */
operator fun <T : Temporal?> ExpressionWithColumnType<T>.minus(duration: Duration): ExpressionWithColumnType<T> {
    return plus(-duration)
}

/**
 * This date or timestamp plus [days] calendar days, computed by the database (PostgreSQL or MariaDB).
 *
 * Keeps the column type of this expression. On the library's connections, which run in UTC, a day is always 24 hours.
 */
fun <T : Temporal?> ExpressionWithColumnType<T>.plusDays(days: Int): ExpressionWithColumnType<T> {
    return TemporalShiftOp(this, days.toLong(), TemporalShiftOp.ShiftUnit.DAYS)
}

/**
 * This date or timestamp minus [days] calendar days, computed by the database (PostgreSQL or MariaDB).
 *
 * @see plusDays
 */
fun <T : Temporal?> ExpressionWithColumnType<T>.minusDays(days: Int): ExpressionWithColumnType<T> {
    return TemporalShiftOp(this, -days.toLong(), TemporalShiftOp.ShiftUnit.DAYS)
}
