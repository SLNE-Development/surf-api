package dev.slne.surf.database.testing

import org.testcontainers.containers.JdbcDatabaseContainer
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A database vendor whose test server runs outside UTC, with direct access to stored time values.
 */
enum class OutsideUtcVendor(
    val container: () -> JdbcDatabaseContainer<*>,
    private val utcSessionSql: String,
    private val epochMicrosExpression: String,
) {
    POSTGRESQL(
        DatabaseContainers::postgresOutsideUtc,
        "SET TIME ZONE 'UTC'",
        "(EXTRACT(EPOCH FROM %s) * 1000000)::BIGINT",
    ),
    MARIADB(
        DatabaseContainers::mariadbOutsideUtc,
        "SET time_zone = '+00:00'",
        "CAST(UNIX_TIMESTAMP(%s) * 1000000 AS SIGNED)",
    );

    /**
     * Microseconds since the epoch of [column] in row [id], read over the migration data source of [testDb] in a UTC
     * session, so wall-clock types count as UTC.
     */
    fun epochMicros(testDb: TestDatabase, table: String, column: String, id: Int): Long =
        testDb.migrationDataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(utcSessionSql)
                val expression = epochMicrosExpression.format(column)
                statement.executeQuery("SELECT $expression FROM $table WHERE id = $id").use { rows ->
                    rows.next()
                    rows.getLong(1)
                }
            }
        }

    /**
     * [epochMicros] as an [Instant].
     */
    fun epochMicrosInstant(testDb: TestDatabase, table: String, column: String, id: Int): Instant =
        Instant.EPOCH.plus(epochMicros(testDb, table, column, id), ChronoUnit.MICROS)
}
