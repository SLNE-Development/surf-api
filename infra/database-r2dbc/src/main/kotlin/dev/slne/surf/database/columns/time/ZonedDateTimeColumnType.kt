package dev.slne.surf.database.columns.time

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ZonedDateTimeColumnType :
    UtcInstantDateTimeColumnType<ZonedDateTime>() {

    override fun toInstant(value: ZonedDateTime): Instant =
        value.toInstant()

    override fun fromInstant(instant: Instant): ZonedDateTime =
        instant.atZone(ZoneId.systemDefault())

    companion object {
        internal val INSTANCE = ZonedDateTimeColumnType()
    }
}

fun Table.zonedDateTime(name: String): Column<ZonedDateTime> =
    registerColumn(name, ZonedDateTimeColumnType())

object CurrentZonedDateTime :
    CurrentTimestampBase<ZonedDateTime>(ZonedDateTimeColumnType.INSTANCE)