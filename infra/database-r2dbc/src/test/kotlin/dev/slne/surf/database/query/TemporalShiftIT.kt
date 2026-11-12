package dev.slne.surf.database.query

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.columns.time.CurrentOffsetDateTime
import dev.slne.surf.database.columns.time.offsetDateTime
import dev.slne.surf.database.columns.time.zonedDateTime
import dev.slne.surf.database.testing.OutsideUtcVendor
import dev.slne.surf.database.testing.toDatabaseApi
import dev.slne.surf.database.testing.withJvmZone
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.toJavaDuration

/**
 * Date arithmetic and UTC day bucketing on every time column type, against servers outside UTC and with a JVM
 * default zone far east of UTC.
 */
@Testcontainers(disabledWithoutDocker = true)
class TemporalShiftIT {

    private class TimeRows : Table("time_rows") {
        val id = integer("id")
        val library = offsetDateTime("library_at")
        val zoned = zonedDateTime("zoned_at")
        val exposed = timestamp("exposed_at")
        val local = datetime("local_at")
        val day = date("day_on")

        override val primaryKey = PrimaryKey(id)
    }

    private class OffsetRows : Table("offset_rows") {
        val id = integer("id")
        val value = timestampWithTimeZone("value_at")

        override val primaryKey = PrimaryKey(id)
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun shiftsEveryTimestampColumnByADuration(vendor: OutsideUtcVendor) = withRows(vendor) { rows ->
        val forward = 1.days + 90.minutes + 1.microseconds
        val backward = 36.hours + 250.microseconds

        assertShift(rows, rows.library, forward, backward) { it.toInstant() }
        assertShift(rows, rows.zoned, forward, backward) { it.toInstant() }
        assertShift(rows, rows.exposed, forward, backward) { it }
        assertShift(rows, rows.local, forward, backward) { it.toInstant(ZoneOffset.UTC) }
    }

    @Test
    fun shiftsTimestampWithTimeZoneOnPostgres() = withApi(OutsideUtcVendor.POSTGRESQL) {
        val rows = OffsetRows()
        SchemaUtils.create(rows)
        rows.insert {
            it[id] = 1
            it[value] = OffsetDateTime.ofInstant(LATE_EVENING, ZoneOffset.ofHours(2))
        }

        val shifted = rows.value + 2.hours
        val earlier = rows.value.minusDays(3)
        val utcDay = rows.value.date()
        val row = rows.select(shifted, earlier, utcDay).single()

        assertEquals(LATE_EVENING + 2.hours.toJavaDuration(), row[shifted].toInstant())
        assertEquals(LATE_EVENING - 3.days.toJavaDuration(), row[earlier].toInstant())
        assertEquals(UTC_DAY, row[utcDay])
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun shiftsDatesAndTimestampsByDays(vendor: OutsideUtcVendor) = withRows(vendor) { rows ->
        val nextDay = rows.day.plusDays(1)
        val earlierDay = rows.day.minusDays(40)
        val nextTimestamp = rows.library.plusDays(1)
        val earlierTimestamp = rows.exposed.minusDays(400)
        val row = rows.select(nextDay, earlierDay, nextTimestamp, earlierTimestamp).single()

        assertEquals(UTC_DAY.plusDays(1), row[nextDay])
        assertEquals(UTC_DAY.minusDays(40), row[earlierDay])
        assertEquals(LATE_EVENING + 1.days.toJavaDuration(), row[nextTimestamp].toInstant())
        assertEquals(LATE_EVENING - 400.days.toJavaDuration(), row[earlierTimestamp])
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun filtersWithAShiftedColumn(vendor: OutsideUtcVendor) = withRows(vendor) { rows ->
        val cutoff = OffsetDateTime.ofInstant(LATE_EVENING + 1.hours.toJavaDuration(), ZoneOffset.UTC)

        val beforeCutoff = rows.selectAll().where { (rows.library + 59.minutes) less cutoff }.count()
        val atOrAfterCutoff = rows.selectAll().where { (rows.library + 1.hours) greaterEq cutoff }.count()

        val withinAYear = rows.selectAll().where { rows.library greaterEq (CurrentOffsetDateTime() - 365.days) }.count()

        assertEquals(1, beforeCutoff)
        assertEquals(1, atOrAfterCutoff)
        assertEquals(1, withinAYear)
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun groupsByADayOfAShiftedTimestamp(vendor: OutsideUtcVendor) = withRows(vendor) { rows ->
        rows.insertRow(id = 2, instant = LATE_EVENING - 2.hours.toJavaDuration())

        val shiftedDay = (rows.library + 1.hours).date()
        val count = rows.id.count()
        val countsByDay = rows.select(shiftedDay, count)
            .groupBy(shiftedDay)
            .map { it[shiftedDay] to it[count] }
            .toList()
            .toMap()

        assertEquals(mapOf(UTC_DAY to 1L, UTC_DAY.plusDays(1) to 1L), countsByDay)
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun exposedDateBucketsByTheUtcDay(vendor: OutsideUtcVendor) = withRows(vendor) { rows ->
        val libraryDay = rows.library.date()
        val zonedDay = rows.zoned.date()
        val exposedDay = rows.exposed.date()
        val localDay = rows.local.date()
        val row = rows.select(libraryDay, zonedDay, exposedDay, localDay).single()

        assertEquals(UTC_DAY, row[libraryDay])
        assertEquals(UTC_DAY, row[zonedDay])
        assertEquals(UTC_DAY, row[exposedDay])
        assertEquals(UTC_DAY, row[localDay])
    }

    @Test
    fun rejectsDurationsOnDateColumnsAndInfiniteDurations() {
        val rows = TimeRows()

        assertFailsWith<IllegalArgumentException> { rows.day + 1.hours }
        assertFailsWith<IllegalArgumentException> { rows.library + Duration.INFINITE }
        assertFailsWith<IllegalArgumentException> { rows.library - Duration.INFINITE }
    }

    @Test
    fun dropsTheSubMicrosecondPart() = withApi(OutsideUtcVendor.POSTGRESQL) {
        val rows = TimeRows()
        SchemaUtils.create(rows)
        rows.insertRow(id = 1, instant = LATE_EVENING)

        val shifted = rows.library + (1.microseconds + 999.nanoseconds)

        assertEquals(LATE_EVENING + 1.microseconds.toJavaDuration(), rows.select(shifted).single()[shifted].toInstant())
    }

    private suspend fun <T : Temporal> assertShift(
        rows: TimeRows,
        column: ExpressionWithColumnType<T>,
        forward: Duration,
        backward: Duration,
        toInstant: (T) -> Instant,
    ) {
        val later = column + forward
        val earlier = column - backward
        val row = rows.select(later, earlier).single()

        assertEquals(LATE_EVENING + forward.toJavaDuration(), toInstant(row[later]), "$column + $forward")
        assertEquals(LATE_EVENING - backward.toJavaDuration(), toInstant(row[earlier]), "$column - $backward")
    }

    private fun withRows(vendor: OutsideUtcVendor, block: suspend R2dbcTransaction.(TimeRows) -> Unit) =
        withApi(vendor) {
            val rows = TimeRows()
            SchemaUtils.create(rows)
            rows.insertRow(id = 1, instant = LATE_EVENING)
            block(rows)
        }

    private suspend fun TimeRows.insertRow(id: Int, instant: Instant) {
        insert {
            it[this.id] = id
            it[library] = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
            it[zoned] = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
            it[exposed] = instant
            it[local] = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
            it[day] = LocalDate.ofInstant(instant, ZoneOffset.UTC)
        }
    }

    private fun withApi(vendor: OutsideUtcVendor, block: suspend R2dbcTransaction.() -> Unit) =
        withJvmZone(JVM_ZONE) {
            val api: DatabaseApi = vendor.container().toDatabaseApi(fresh = true)
            try {
                suspendTransaction(api.database) { block() }
            } finally {
                api.shutdown()
            }
        }

    private companion object {
        /**
         * UTC+14: every instant of [LATE_EVENING]'s UTC day falls on the next day in this zone.
         */
        const val JVM_ZONE = "Pacific/Kiritimati"

        val LATE_EVENING: Instant = Instant.parse("2026-03-28T23:30:00.123456Z")
        val UTC_DAY: LocalDate = LocalDate.parse("2026-03-28")
    }
}
