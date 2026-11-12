package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.testing.OutsideUtcVendor
import dev.slne.surf.database.testing.toDatabaseApi
import dev.slne.surf.database.testing.withJvmZone
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins that migrations run by Flyway see the session time zone UTC, with the JVM and the server outside UTC.
 *
 * Unlike [JdbcDataSourcesIT], the migrations here go through the Flyway runner and its Java migration adapter.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayTimeZoneIT {

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun sqlMigrationRunsInUtcSession(vendor: OutsideUtcVendor) = withMigratedProbe(vendor) { api, before, after ->
        assertEquals(vendor.sessionZone, api.probeZone(SQL_ROW))
        assertWithin(before, after, vendor.epochMicrosInstant(api, "zone_probe", "wall_clock", SQL_ROW))
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun javaMigrationRunsInUtcSession(vendor: OutsideUtcVendor) = withMigratedProbe(vendor) { api, before, after ->
        assertEquals(vendor.sessionZone, api.probeZone(JAVA_ROW))
        assertWithin(before, after, vendor.epochMicrosInstant(api, "zone_probe", "wall_clock", JAVA_ROW))
    }

    /**
     * Migrates a fresh database with the probe SQL migration and [ZoneProbeJdbcMigration] while the JVM zone is
     * [JVM_ZONE], then calls [block] with the API and the instants bracketing the run.
     */
    private fun withMigratedProbe(
        vendor: OutsideUtcVendor,
        block: (api: DatabaseApi, before: Instant, after: Instant) -> Unit,
    ) = withJvmZone(JVM_ZONE) {
        val api = vendor.container().toDatabaseApi(fresh = true)
        try {
            val before = Instant.now()
            api.migrate("flyway_zone") {
                sql("migrations/flyway_zone/{vendor}")
                classes(ZoneProbeJdbcMigration(vendor))
            }
            block(api, before, Instant.now())
        } finally {
            api.shutdown()
        }
    }

    /**
     * Fails if [stored] is not between [before] and [after], widened by [TOLERANCE]. A session zone other than UTC
     * shifts the stored wall-clock time by hours.
     */
    private fun assertWithin(before: Instant, after: Instant, stored: Instant) {
        assertTrue(
            stored >= before - TOLERANCE && stored <= after + TOLERANCE,
            "Stored wall-clock time $stored is not within $TOLERANCE of [$before, $after]"
        )
    }

    private fun DatabaseApi.probeZone(id: Int): String =
        assertNotNull(migrationDataSource).connection.use { connection ->
            connection.prepareStatement("SELECT session_zone FROM zone_probe WHERE id = ?").use { statement ->
                statement.setInt(1, id)
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next(), "No zone_probe row with id $id")
                    rows.getString(1)
                }
            }
        }

    /**
     * Stores the session time zone and the server's current wall-clock time, as the SQL migration does.
     */
    private class ZoneProbeJdbcMigration(private val vendor: OutsideUtcVendor) :
        JdbcMigration("2", "zone probe jdbc") {

        override fun migrate(connection: Connection) {
            val (zoneExpression, nowExpression) = when (vendor) {
                OutsideUtcVendor.MARIADB -> "@@session.time_zone" to "NOW(6)"
                OutsideUtcVendor.POSTGRESQL -> "current_setting('TimeZone')" to "LOCALTIMESTAMP"
            }
            connection.createStatement().use {
                it.execute(
                    "INSERT INTO zone_probe (id, source, session_zone, wall_clock) " +
                        "VALUES ($JAVA_ROW, 'java', $zoneExpression, $nowExpression)"
                )
            }
        }
    }

    private val OutsideUtcVendor.sessionZone: String
        get() = when (this) {
            OutsideUtcVendor.MARIADB -> "+00:00"
            OutsideUtcVendor.POSTGRESQL -> "UTC"
        }

    private companion object {
        const val JVM_ZONE = "America/New_York"
        const val SQL_ROW = 1
        const val JAVA_ROW = 2
        val TOLERANCE: Duration = Duration.ofMinutes(5)
    }
}
