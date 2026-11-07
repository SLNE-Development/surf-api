package dev.slne.surf.database.migration

import dev.slne.surf.database.testing.OutsideUtcVendor
import dev.slne.surf.database.testing.toDatabaseApi
import dev.slne.surf.database.testing.withJvmZone
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Pins the README's `JdbcMigration` guidance: how the JDBC drivers bind instants on the migration data source while
 * the JVM runs outside UTC.
 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcTimeBindingIT {

    private val instant = Instant.parse("2030-06-01T12:00:00.123456Z")

    /**
     * Offset of [JVM_ZONE] at [instant]: a value converted through the JVM zone is stored this much early.
     */
    private val jvmShift = Duration.ofHours(-4)

    private fun bindings(): Map<String, (PreparedStatement, Int) -> Unit> = mapOf(
        "Timestamp" to { statement, index -> statement.setTimestamp(index, Timestamp.from(instant)) },
        "OffsetDateTime" to { statement, index -> statement.setObject(index, instant.atOffset(ZoneOffset.ofHours(2))) },
        "UTC LocalDateTime" to { statement, index ->
            statement.setObject(index, LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
        },
    )

    /**
     * Stored shift per binding and column, as `binding/column`.
     */
    private fun storedShifts(vendor: OutsideUtcVendor): Map<String, Duration> {
        val shifts = linkedMapOf<String, Duration>()
        withJvmZone(JVM_ZONE) {
            val api = vendor.container().toDatabaseApi(fresh = true)
            try {
                assertNotNull(api.migrationDataSource).connection.use { connection ->
                    connection.createStatement().use { it.execute(vendor.createTable()) }
                    bindings().entries.forEachIndexed { id, (name, bind) ->
                        connection.prepareStatement("INSERT INTO bound (id, plain, with_zone) VALUES (?, ?, ?)").use {
                            it.setInt(1, id)
                            bind(it, 2)
                            bind(it, 3)
                            it.executeUpdate()
                        }
                        for (column in listOf("plain", "with_zone")) {
                            val stored = vendor.epochMicrosInstant(api, "bound", column, id)
                            shifts["$name/$column"] = Duration.between(instant, stored)
                        }
                    }
                }
            } finally {
                api.shutdown()
            }
        }
        return shifts
    }

    private fun OutsideUtcVendor.createTable(): String = when (this) {
        OutsideUtcVendor.MARIADB ->
            "CREATE TABLE bound (id INT PRIMARY KEY, plain DATETIME(6), with_zone TIMESTAMP(6) NULL)"

        OutsideUtcVendor.POSTGRESQL ->
            "CREATE TABLE bound (id INT PRIMARY KEY, plain TIMESTAMP, with_zone TIMESTAMPTZ)"
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun jdbcParametersConvertThroughTheJvmZoneExceptUtcLocalDateTime(vendor: OutsideUtcVendor) {
        val expected = when (vendor) {
            OutsideUtcVendor.MARIADB -> mapOf(
                "Timestamp/plain" to jvmShift,
                "Timestamp/with_zone" to jvmShift,
                "OffsetDateTime/plain" to jvmShift,
                "OffsetDateTime/with_zone" to jvmShift,
            )

            OutsideUtcVendor.POSTGRESQL -> mapOf("Timestamp/plain" to jvmShift)
        }

        val shifts = storedShifts(vendor)

        assertEquals(expected, shifts.filterValues { !it.isZero })
        assertEquals(Duration.ZERO, shifts["UTC LocalDateTime/plain"])
        assertEquals(Duration.ZERO, shifts["UTC LocalDateTime/with_zone"])
    }

    private companion object {
        /**
         * UTC-4 in June, the month of [instant].
         */
        const val JVM_ZONE = "America/New_York"
    }
}
