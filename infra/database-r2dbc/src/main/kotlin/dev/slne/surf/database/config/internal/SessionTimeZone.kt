package dev.slne.surf.database.config.internal

/**
 * Session time zone of every R2DBC and JDBC connection the library opens.
 *
 * The time column types and [UtcSessionTypeMapper][dev.slne.surf.database.columns.time.internal.UtcSessionTypeMapper]
 * exchange instants as UTC wall-clock values, and the database interprets those values, and evaluates
 * `CURRENT_TIMESTAMP`, in the session time zone. A fixed offset without daylight saving time is required: in a zone
 * with DST, MariaDB `TIMESTAMP` values in the repeated hour read back as the same wall-clock value.
 */
internal const val SESSION_TIME_ZONE = "UTC"
