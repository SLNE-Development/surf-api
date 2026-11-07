package dev.slne.surf.database.columns.time

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.OutsideUtcVendor
import dev.slne.surf.database.testing.connectionDetails
import dev.slne.surf.database.testing.toDatabaseApi
import dev.slne.surf.database.testing.toLegacyDatabaseApi
import dev.slne.surf.database.testing.withJvmZone
import kotlinx.coroutines.flow.single
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Rows written by version 2.3 of the library read as the README's upgrade notes describe, and the README's fix
 * statements restore them.
 *
 * Version 2.3 is emulated by [toLegacyDatabaseApi] on the same database. The fix statements are the README's
 * templates with only `t`, `c`, `'J'` and `'S'` substituted, run over the migration data source in a UTC session.
 */
@Testcontainers(disabledWithoutDocker = true)
class TimeZoneUpgradeIT {

    /**
     * One column per Exposed type and column flavour. `plain` is `timestamp` (PostgreSQL) or `DATETIME(6)`
     * (MariaDB), `zoned` is `timestamptz` or `TIMESTAMP(6)`. Row [APP_ROW] holds values the application wrote,
     * row [DEFAULT_ROW] the columns' `CURRENT_TIMESTAMP` defaults.
     */
    private class LegacyTimes : Table(TABLE) {
        val id = integer("id")
        val libraryPlain = offsetDateTime("library_plain")
        val libraryZoned = offsetDateTime("library_zoned")
        val exposedPlain = timestamp("exposed_plain")
        val exposedZoned = timestamp("exposed_zoned")
        val localPlain = datetime("local_plain")
        val localZoned = datetime("local_zoned")

        override val primaryKey = PrimaryKey(id)

        /**
         * The instant of every column of [row], reading `datetime()` values as UTC wall-clock time.
         */
        fun instants(row: ResultRow): Map<String, Instant> = mapOf(
            libraryPlain.name to row[libraryPlain].toInstant(),
            libraryZoned.name to row[libraryZoned].toInstant(),
            exposedPlain.name to row[exposedPlain],
            exposedZoned.name to row[exposedZoned],
            localPlain.name to row[localPlain].toInstant(ZoneOffset.UTC),
            localZoned.name to row[localZoned].toInstant(ZoneOffset.UTC),
        )
    }

    private class LegacyAuditedRows : AuditableTable(AUDITED_TABLE) {
        val id = integer("id")
        val expiresAt = timestamp("expires_at")

        override val primaryKey = PrimaryKey(id)
    }

    private class LegacyOffsets : Table("legacy_offsets") {
        val id = integer("id")
        val value = timestampWithTimeZone("value")

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * How [column] in [row] reads after the upgrade, relative to the value written, and the README fix for it.
     */
    private data class Expectation(val column: String, val row: Int, val shift: Duration, val fix: String? = null)

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun legacyRowsReadAsDocumentedAndTheFixesRestoreThem(vendor: OutsideUtcVendor) = withJvmZone(JVM_ZONE) {
        val details = vendor.container().connectionDetails(fresh = true)
        val table = LegacyTimes()

        val writtenAt = Instant.now()
        writeAsLegacy(details) {
            exec(createLegacyTimes(vendor))
            table.insert {
                it[id] = APP_ROW
                it[libraryPlain] = REGULAR.atOffset(JVM_OFFSET)
                it[libraryZoned] = REGULAR.atOffset(JVM_OFFSET)
                it[exposedPlain] = REGULAR
                it[exposedZoned] = REGULAR
                it[localPlain] = WALL_CLOCK
                it[localZoned] = WALL_CLOCK
            }
            exec("INSERT INTO $TABLE (id) VALUES ($DEFAULT_ROW)")
        }

        val api = details.toDatabaseApi()
        try {
            val expectations = expectations(vendor)
            assertRows(api, table, expectations, writtenAt, afterFix = false)

            for (expectation in expectations) {
                val fix = expectation.fix ?: continue
                val statement = vendor.substitute(fix, TABLE, expectation.column)
                executeInUtcSession(api, "$statement WHERE id = ${expectation.row}")
            }
            assertRows(api, table, expectations, writtenAt, afterFix = true)
        } finally {
            api.shutdown()
        }
    }

    @Test
    fun postgresTimestampWithTimeZoneIsUnchanged() = withJvmZone(JVM_ZONE) {
        val details = OutsideUtcVendor.POSTGRESQL.container().connectionDetails(fresh = true)
        val table = LegacyOffsets()
        writeAsLegacy(details) {
            SchemaUtils.create(table)
            table.insert {
                it[id] = APP_ROW
                it[value] = REGULAR.atOffset(JVM_OFFSET)
            }
        }

        val api = details.toDatabaseApi()
        try {
            val row = suspendTransaction(api.database) { table.selectAll().single() }
            assertEquals(REGULAR, row[table.value].toInstant())
        } finally {
            api.shutdown()
        }
    }

    @ParameterizedTest
    @EnumSource(OutsideUtcVendor::class)
    fun auditableFixKeepsUpdatedAt(vendor: OutsideUtcVendor) = withJvmZone(JVM_ZONE) {
        val details = vendor.container().connectionDetails(fresh = true)
        val table = writeLegacyAuditedRow(details, vendor)

        val api = details.toDatabaseApi()
        try {
            if (vendor == OutsideUtcVendor.POSTGRESQL) {
                executeInUtcSession(api, POSTGRESQL_CREATE_TRIGGER.replace("<table>", AUDITED_TABLE))
            }
            val fix = when (vendor) {
                OutsideUtcVendor.POSTGRESQL -> POSTGRESQL_AUDITABLE_FIX
                OutsideUtcVendor.MARIADB -> MARIADB_AUDITABLE_FIX
            }
            val updatedAtBefore = vendor.epochMicros(api, AUDITED_TABLE, "updated_at", APP_ROW)

            val statements = vendor.substitute(fix, AUDITED_TABLE, "expires_at").split(";\n")
            executeInUtcSession(api, *statements.toTypedArray())

            assertFixedAndUpdatedAtKept(api, vendor, table, updatedAtBefore)
            suspendTransaction(api.database) {
                table.update({ table.id eq APP_ROW }) { it[expiresAt] = REGULAR.plusSeconds(1) }
            }
            assertTrue(vendor.epochMicros(api, AUDITED_TABLE, "updated_at", APP_ROW) > updatedAtBefore)
        } finally {
            api.shutdown()
        }
    }

    @Test
    fun postgresFixBeforeCreatingTheTriggerKeepsUpdatedAt() = withJvmZone(JVM_ZONE) {
        val vendor = OutsideUtcVendor.POSTGRESQL
        val details = vendor.container().connectionDetails(fresh = true)
        val table = writeLegacyAuditedRow(details, vendor)

        val api = details.toDatabaseApi()
        try {
            val updatedAtBefore = vendor.epochMicros(api, AUDITED_TABLE, "updated_at", APP_ROW)

            executeInUtcSession(api, vendor.substitute(POSTGRESQL_FIX, AUDITED_TABLE, "expires_at"))

            assertFixedAndUpdatedAtKept(api, vendor, table, updatedAtBefore)
        } finally {
            api.shutdown()
        }
    }

    /**
     * Creates [LegacyAuditedRows] as version 2.3 left it and writes row [APP_ROW] with it.
     */
    private suspend fun writeLegacyAuditedRow(details: ConnectionDetails, vendor: OutsideUtcVendor): LegacyAuditedRows {
        val table = LegacyAuditedRows()
        writeAsLegacy(details) {
            SchemaUtils.create(table)
            // Version 2.3 dropped the trigger right after creating it.
            if (vendor == OutsideUtcVendor.POSTGRESQL) {
                exec("DROP TRIGGER ${AUDITED_TABLE}_set_updated_at ON $AUDITED_TABLE")
            }
            table.insert {
                it[id] = APP_ROW
                it[expiresAt] = REGULAR
            }
        }
        return table
    }

    private suspend fun assertFixedAndUpdatedAtKept(
        api: DatabaseApi,
        vendor: OutsideUtcVendor,
        table: LegacyAuditedRows,
        updatedAtBefore: Long,
    ) {
        assertEquals(updatedAtBefore, vendor.epochMicros(api, AUDITED_TABLE, "updated_at", APP_ROW))
        val fixed = suspendTransaction(api.database) {
            table.selectAll().where { table.id eq APP_ROW }.single()
        }
        assertEquals(REGULAR, fixed[table.expiresAt])
    }

    private suspend fun writeAsLegacy(details: ConnectionDetails, block: suspend R2dbcTransaction.() -> Unit) {
        val legacy = details.toLegacyDatabaseApi()
        try {
            suspendTransaction(legacy.database) { block() }
        } finally {
            legacy.shutdown()
        }
    }

    private suspend fun assertRows(
        api: DatabaseApi,
        table: LegacyTimes,
        expectations: List<Expectation>,
        writtenAt: Instant,
        afterFix: Boolean,
    ) {
        val rows = suspendTransaction(api.database) {
            listOf(APP_ROW, DEFAULT_ROW).associateWith { id ->
                table.instants(table.selectAll().where { table.id eq id }.single())
            }
        }

        for (expectation in expectations) {
            val read = assertNotNull(rows.getValue(expectation.row)[expectation.column])
            val shift = if (afterFix) Duration.ZERO else expectation.shift
            val stage = if (afterFix) "after the fix" else "before"
            val label = "${expectation.column} in row ${expectation.row}, $stage"

            if (expectation.row == APP_ROW) {
                assertEquals(REGULAR.plus(shift), read, label)
            } else {
                val drift = Duration.between(writtenAt.plus(shift), read).abs()
                assertTrue(drift < MAX_DRIFT, "$label: $read is $drift away from the expected time")
            }
        }
    }

    private fun executeInUtcSession(api: DatabaseApi, vararg statements: String) {
        assertNotNull(api.migrationDataSource).connection.use { connection ->
            connection.autoCommit = false
            connection.createStatement().use { statement ->
                statements.forEach(statement::execute)
            }
            connection.commit()
        }
    }

    private fun OutsideUtcVendor.substitute(template: String, table: String, column: String): String {
        // CONVERT_TZ accepts named zones only with the MariaDB time zone tables loaded.
        val jvmZone = if (this == OutsideUtcVendor.MARIADB) JVM_OFFSET.id else JVM_ZONE
        return template
            .replace("t_set_updated_at", "${table}_set_updated_at")
            .replace(Regex("\\bt\\b"), table)
            .replace(Regex("\\bc\\b"), column)
            .replace("'J'", "'$jvmZone'")
            .replace("'S'", "'${DatabaseContainers.SERVER_TIME_ZONE}'")
    }

    private fun createLegacyTimes(vendor: OutsideUtcVendor): String = when (vendor) {
        OutsideUtcVendor.POSTGRESQL -> """
            CREATE TABLE $TABLE (
                id            INT         NOT NULL PRIMARY KEY,
                library_plain TIMESTAMP   NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
                library_zoned TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                exposed_plain TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                exposed_zoned TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                local_plain   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                local_zoned   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        OutsideUtcVendor.MARIADB -> """
            CREATE TABLE $TABLE (
                id            INT          NOT NULL PRIMARY KEY,
                library_plain DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                library_zoned TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                exposed_plain DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                exposed_zoned TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                local_plain   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                local_zoned   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
            )
        """.trimIndent()
    }

    /**
     * The README's upgrade table for [vendor], with J = [JVM_ZONE] and S = the MariaDB server zone.
     */
    private fun expectations(vendor: OutsideUtcVendor): List<Expectation> {
        val jvm = Duration.ofSeconds(JVM_OFFSET.totalSeconds.toLong())
        val server = Duration.ofSeconds(MARIADB_SERVER_OFFSET.totalSeconds.toLong())

        return when (vendor) {
            OutsideUtcVendor.POSTGRESQL -> listOf(
                Expectation("library_plain", APP_ROW, Duration.ZERO),
                Expectation("library_plain", DEFAULT_ROW, Duration.ZERO),
                Expectation("library_zoned", APP_ROW, jvm.negated(), POSTGRESQL_FIX),
                Expectation("library_zoned", DEFAULT_ROW, Duration.ZERO),
                Expectation("exposed_plain", APP_ROW, jvm, POSTGRESQL_FIX),
                Expectation("exposed_plain", DEFAULT_ROW, jvm, POSTGRESQL_FIX),
                Expectation("exposed_zoned", APP_ROW, Duration.ZERO),
                Expectation("exposed_zoned", DEFAULT_ROW, Duration.ZERO),
                Expectation("local_plain", APP_ROW, Duration.ZERO),
                Expectation("local_plain", DEFAULT_ROW, jvm, POSTGRESQL_FIX),
                Expectation("local_zoned", APP_ROW, jvm.negated(), POSTGRESQL_FIX),
                Expectation("local_zoned", DEFAULT_ROW, Duration.ZERO),
            )

            OutsideUtcVendor.MARIADB -> listOf(
                Expectation("library_plain", APP_ROW, Duration.ZERO),
                Expectation("library_plain", DEFAULT_ROW, server, MARIADB_SERVER_WALL_CLOCK_FIX),
                Expectation("library_zoned", APP_ROW, server.negated(), MARIADB_SERVER_SESSION_FIX),
                Expectation("library_zoned", DEFAULT_ROW, Duration.ZERO),
                Expectation("exposed_plain", APP_ROW, jvm, MARIADB_JVM_WALL_CLOCK_FIX),
                Expectation("exposed_plain", DEFAULT_ROW, server, MARIADB_SERVER_WALL_CLOCK_FIX),
                Expectation("exposed_zoned", APP_ROW, jvm.minus(server), MARIADB_JVM_IN_SERVER_SESSION_FIX),
                Expectation("exposed_zoned", DEFAULT_ROW, Duration.ZERO),
                Expectation("local_plain", APP_ROW, Duration.ZERO),
                Expectation("local_plain", DEFAULT_ROW, server, MARIADB_SERVER_WALL_CLOCK_FIX),
                Expectation("local_zoned", APP_ROW, server.negated(), MARIADB_SERVER_SESSION_FIX),
                Expectation("local_zoned", DEFAULT_ROW, Duration.ZERO),
            )
        }
    }

    companion object {
        /**
         * JVM default zone of the emulated version 2.3 application; without daylight saving time.
         */
        const val JVM_ZONE = "Asia/Kolkata"
        val JVM_OFFSET: ZoneOffset = ZoneId.of(JVM_ZONE).rules.getOffset(Instant.EPOCH)
        val MARIADB_SERVER_OFFSET: ZoneOffset = ZoneOffset.of(DatabaseContainers.SERVER_TIME_ZONE)

        const val TABLE = "legacy_times"
        const val AUDITED_TABLE = "legacy_audited"
        const val APP_ROW = 1
        const val DEFAULT_ROW = 2

        val REGULAR: Instant = Instant.parse("2030-06-01T12:00:00.123456Z")
        val WALL_CLOCK: LocalDateTime = LocalDateTime.ofInstant(REGULAR, ZoneOffset.UTC)
        val MAX_DRIFT: Duration = Duration.ofMinutes(1)

        // Fix templates, verbatim from the README's upgrade notes.
        const val POSTGRESQL_FIX = "UPDATE t SET c = (c AT TIME ZONE 'J') AT TIME ZONE 'UTC'"
        const val MARIADB_SERVER_SESSION_FIX = "UPDATE t SET c = CONVERT_TZ(c, '+00:00', 'S')"
        const val MARIADB_JVM_IN_SERVER_SESSION_FIX =
            "UPDATE t SET c = CONVERT_TZ(CONVERT_TZ(c, '+00:00', 'S'), 'J', '+00:00')"
        const val MARIADB_SERVER_WALL_CLOCK_FIX = "UPDATE t SET c = CONVERT_TZ(c, 'S', '+00:00')"
        const val MARIADB_JVM_WALL_CLOCK_FIX = "UPDATE t SET c = CONVERT_TZ(c, 'J', '+00:00')"

        const val MARIADB_AUDITABLE_FIX =
            "UPDATE t SET c = CONVERT_TZ(CONVERT_TZ(c, '+00:00', 'S'), 'J', '+00:00'), updated_at = updated_at"
        const val POSTGRESQL_AUDITABLE_FIX = """ALTER TABLE t DISABLE TRIGGER t_set_updated_at;
UPDATE t SET c = (c AT TIME ZONE 'J') AT TIME ZONE 'UTC';
ALTER TABLE t ENABLE TRIGGER t_set_updated_at;"""

        const val POSTGRESQL_CREATE_TRIGGER = """CREATE TRIGGER <table>_set_updated_at BEFORE UPDATE ON <table>
    FOR EACH ROW EXECUTE FUNCTION surf_set_updated_at();"""
    }
}
