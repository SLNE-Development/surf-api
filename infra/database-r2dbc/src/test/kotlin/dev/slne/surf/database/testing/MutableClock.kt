package dev.slne.surf.database.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * A UTC clock that only moves when [advance] is called. Starts at microsecond precision, like the database columns.
 */
class MutableClock(start: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)) : Clock() {
    @Volatile
    private var now: Instant = start

    fun advance(duration: Duration) {
        now += duration.toJavaDuration()
    }

    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}
