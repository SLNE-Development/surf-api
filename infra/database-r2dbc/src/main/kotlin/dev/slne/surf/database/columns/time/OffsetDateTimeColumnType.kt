package dev.slne.surf.database.columns.time

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class OffsetDateTimeColumnType :
    UtcInstantDateTimeColumnType<OffsetDateTime>() {

    override fun toInstant(value: OffsetDateTime): Instant =
        value.toInstant()

    override fun fromInstant(instant: Instant): OffsetDateTime =
        instant.atOffset(ZoneOffset.UTC)

    companion object {
        internal val INSTANCE = OffsetDateTimeColumnType()
    }
}

fun Table.offsetDateTime(name: String): Column<OffsetDateTime> =
    registerColumn(name, OffsetDateTimeColumnType())

object CurrentOffsetDateTime :
    CurrentTimestampBase<OffsetDateTime>(OffsetDateTimeColumnType.INSTANCE)