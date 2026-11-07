package dev.slne.surf.database.columns.time

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.toDatabaseApi
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Time columns keep their instants when neither the JVM default zone nor the MariaDB server zone is UTC.
 *
 * The JVM default zone is global, so each test changes it and restores it afterwards. It is set before the API is
 * created because the PostgreSQL driver would otherwise take its session time zone from it.
 */
@Testcontainers(disabledWithoutDocker = true)
class SessionTimeZoneIT {

    private class Events : Table("tz_events") {
        val id = integer("id")
        val occurredAt = offsetDateTime("occurred_at")
        val createdAt = offsetDateTime("created_at").defaultExpression(CurrentOffsetDateTime.WithoutUpdate)

        override val primaryKey = PrimaryKey(id)
    }

    @ParameterizedTest
    @ValueSource(strings = [JVM_ZONE_WEST, JVM_ZONE_EAST])
    fun postgresTimestamptzKeepsTheInstant(jvmZone: String) = withJvmZone(jvmZone) {
        val api = DatabaseContainers.postgres().toDatabaseApi(fresh = true)
        api.useWithTable(
            """
            CREATE TABLE tz_events
            (
                id          INT PRIMARY KEY,
                occurred_at TIMESTAMPTZ NOT NULL,
                created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        ) {
            assertRoundTrip(api, epochQuery = "SELECT EXTRACT(EPOCH FROM occurred_at)::BIGINT FROM tz_events")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [JVM_ZONE_WEST, JVM_ZONE_EAST])
    fun mariaDbOutsideUtcKeepsTheInstant(jvmZone: String) = withJvmZone(jvmZone) {
        val api = DatabaseContainers.mariadbOutsideUtc().toDatabaseApi(fresh = true)
        api.useWithTable(
            """
            CREATE TABLE tz_events
            (
                id          INT PRIMARY KEY,
                occurred_at TIMESTAMP(6) NOT NULL,
                created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
            )
            """
        ) {
            assertRoundTrip(api, epochQuery = "SELECT UNIX_TIMESTAMP(occurred_at) FROM tz_events")
        }
    }

    @Test
    fun mariaDbServerIsOutsideUtc() {
        val container = DatabaseContainers.mariadbOutsideUtc()
        val zone = container.createConnection("").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT @@global.time_zone").use { rows -> rows.next(); rows.getString(1) }
            }
        }

        assertEquals(DatabaseContainers.SERVER_TIME_ZONE, zone)
        assertNotEquals("+00:00", zone)
    }

    private suspend fun assertRoundTrip(api: DatabaseApi, epochQuery: String) {
        val events = Events()
        val before = Instant.now()

        val row = suspendTransaction(api.database) {
            events.insert {
                it[id] = 1
                it[occurredAt] = OffsetDateTime.ofInstant(OCCURRED, TimeZone.getDefault().toZoneId())
            }
            events.selectAll().single()
        }

        assertEquals(OCCURRED, row[events.occurredAt].toInstant())
        assertEquals(OCCURRED.epochSecond, api.queryLong(epochQuery))

        val createdAt = row[events.createdAt].toInstant()
        val drift = Duration.between(before, createdAt).abs()
        assertTrue(drift < MAX_DEFAULT_DRIFT, "created_at $createdAt is $drift away from $before")
    }

    private inline fun DatabaseApi.useWithTable(ddl: String, block: () -> Unit) = try {
        execute(ddl.trimIndent())
        block()
    } finally {
        shutdown()
    }

    private fun DatabaseApi.execute(sql: String) {
        assertNotNull(migrationDataSource).connection.use { c -> c.createStatement().use { it.execute(sql) } }
    }

    private fun DatabaseApi.queryLong(sql: String): Long =
        assertNotNull(migrationDataSource).connection.use { c ->
            c.createStatement().use { s -> s.executeQuery(sql).use { rows -> rows.next(); rows.getLong(1) } }
        }

    private fun withJvmZone(zone: String, block: suspend () -> Unit) {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        try {
            runBlocking { block() }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    private companion object {
        const val JVM_ZONE_WEST = "America/New_York"
        const val JVM_ZONE_EAST = "Asia/Kolkata"
        val OCCURRED: Instant = Instant.parse("2030-06-01T12:00:00Z")
        val MAX_DEFAULT_DRIFT: Duration = Duration.ofMinutes(1)
    }
}
