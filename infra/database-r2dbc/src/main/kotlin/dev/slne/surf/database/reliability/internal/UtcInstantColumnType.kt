package dev.slne.surf.database.reliability.internal

import dev.slne.surf.database.columns.time.UtcInstantDateTimeColumnType
import java.time.Instant

/**
 * Exact [Instant] on a `timestamptz` (PostgreSQL) or `DATETIME(6)` (MariaDB) column of the reliability tables.
 */
internal class UtcInstantColumnType : UtcInstantDateTimeColumnType<Instant>() {
    override fun toInstant(value: Instant): Instant = value
    override fun fromInstant(instant: Instant): Instant = instant
}
