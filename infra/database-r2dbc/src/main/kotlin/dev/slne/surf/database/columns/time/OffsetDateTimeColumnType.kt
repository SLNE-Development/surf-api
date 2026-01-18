package dev.slne.surf.database.columns.time

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class OffsetDateTimeColumnType :
    UtcInstantDateTimeColumnType<OffsetDateTime>() {

    override fun toInstant(value: OffsetDateTime): Instant =
        value.toInstant()

    override fun fromInstant(instant: Instant): OffsetDateTime =
        instant.atZone(ZoneId.systemDefault()).toOffsetDateTime()

    companion object {
        internal val INSTANCE = OffsetDateTimeColumnType()
    }
}

fun Table.offsetDateTime(name: String): Column<OffsetDateTime> =
    registerColumn(name, OffsetDateTimeColumnType())

class CurrentOffsetDateTime(includeUpdate: Boolean = false) :
    CurrentTimestampBase<OffsetDateTime>(OffsetDateTimeColumnType.INSTANCE, includeUpdate)