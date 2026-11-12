package dev.slne.surf.database.columns.time

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.migrate
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.database.testing.OutsideUtcVendor
import dev.slne.surf.database.testing.toDatabaseApi
import dev.slne.surf.database.testing.withJvmZone
import kotlinx.coroutines.flow.single
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.CurrentTimestampWithTimeZone
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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every time column type of the library and of Exposed keeps its value on both vendors, whatever the JVM default zone
 * and the server zone, and agrees with values that the database and SQL migrations generate.
 *
 * Both servers run outside UTC, and each case runs with JVM default zones west and east of UTC. Values are checked
 * through the R2DBC round trip and directly in the database over a JDBC session set to UTC.
 */
@Testcontainers(disabledWithoutDocker = true)
class TimeZoneMatrixIT {

    private class InstantColumns : Table("tz_instants") {
        val id = integer("id")
        val library = offsetDateTime("library")
        val zoned = zonedDateTime("zoned")
        val exposed = timestamp("exposed")
        val local = datetime("local")

        override val primaryKey = PrimaryKey(id)
    }

    private class DefaultColumns : Table("tz_defaults") {
        val id = integer("id")
        val library = offsetDateTime("library").defaultExpression(CurrentOffsetDateTime.WithoutUpdate)
        val exposed = timestamp("exposed").defaultExpression(CurrentTimestamp)
        val local = datetime("local").defaultExpression(CurrentDateTime)

        override val primaryKey = PrimaryKey(id)
    }

    private class AuditedRows : AuditableTable("tz_audited") {
        val id = integer("id")
        val note = varchar("note", 16)

        override val primaryKey = PrimaryKey(id)
    }

    private class OffsetColumns : Table("tz_offsets") {
        val id = integer("id")
        val value = timestampWithTimeZone("value")
        val generated = timestampWithTimeZone("generated").defaultExpression(CurrentTimestampWithTimeZone)

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * `tz_sql` from the SQL migration, read with the library's instant type.
     */
    private class SqlRowsAsLibrary : Table("tz_sql") {
        val id = integer("id")
        val plain = offsetDateTime("plain")
        val withZone = offsetDateTime("with_zone")
    }

    /**
     * `tz_sql` from the SQL migration, read with Exposed's instant type.
     */
    private class SqlRowsAsExposed : Table("tz_sql") {
        val id = integer("id")
        val plain = timestamp("plain")
        val withZone = timestamp("with_zone")
    }

    /**
     * `tz_sql` from the SQL migration, read with Exposed's wall-clock type.
     */
    private class SqlRowsAsLocal : Table("tz_sql") {
        val id = integer("id")
        val plain = datetime("plain")
        val withZone = datetime("with_zone")
    }

    @ParameterizedTest(name = "{0}, JVM {1}")
    @MethodSource("matrix")
    fun appWrittenValuesRoundTripAndMatchTheDatabase(vendor: OutsideUtcVendor, jvmZone: String) = withApi(vendor, jvmZone) { api ->
        val table = InstantColumns()
        val rows = transaction(api) {
            SchemaUtils.create(table)
            table.insert {
                it[id] = 1
                it[library] = REGULAR.atJvmOffset()
                it[zoned] = REGULAR.atJvmZone()
                it[exposed] = REGULAR
                it[local] = WALL_CLOCK
            }
            // Exposed's timestamp() loses the repeated DST hour before binding, see UtcSessionTypeMapper.
            table.insert {
                it[id] = 2
                it[library] = REPEATED_HOUR.atJvmOffset()
                it[zoned] = REPEATED_HOUR.atJvmZone()
                it[exposed] = REGULAR
                it[local] = WALL_CLOCK
            }
            table.byId(table.id, 1) to table.byId(table.id, 2)
        }

        for ((row, id, libraryInstant) in listOf(Triple(rows.first, 1, REGULAR), Triple(rows.second, 2, REPEATED_HOUR))) {
            assertEquals(libraryInstant, row[table.library].toInstant())
            assertEquals(libraryInstant, row[table.zoned].toInstant())
            assertEquals(REGULAR, row[table.exposed])
            assertEquals(WALL_CLOCK, row[table.local])

            assertEquals(libraryInstant.epochMicros(), vendor.epochMicros(api, table.tableName, "library", id))
            assertEquals(libraryInstant.epochMicros(), vendor.epochMicros(api, table.tableName, "zoned", id))
            assertEquals(REGULAR.epochMicros(), vendor.epochMicros(api, table.tableName, "exposed", id))
            assertEquals(WALL_CLOCK.utcEpochMicros(), vendor.epochMicros(api, table.tableName, "local", id))
        }
    }

    /**
     * Pins the documented limit of Exposed's `datetime()`: its `toSqlTimestamp()` resolves the wall-clock value in the
     * JVM default zone before any type mapper runs, so a value in that zone's daylight saving gap is stored one hour
     * late. In a JVM zone without daylight saving time the value is kept.
     */
    @ParameterizedTest(name = "{0}, JVM {1}")
    @MethodSource("matrix")
    fun datetimeInTheJvmZonesDaylightSavingGapIsStoredOneHourLate(vendor: OutsideUtcVendor, jvmZone: String) =
        withApi(vendor, jvmZone) { api ->
            val table = InstantColumns()
            val row = transaction(api) {
                SchemaUtils.create(table)
                table.insert {
                    it[id] = 1
                    it[library] = REGULAR.atJvmOffset()
                    it[zoned] = REGULAR.atJvmZone()
                    it[exposed] = REGULAR
                    it[local] = SKIPPED_HOUR_WALL_CLOCK
                }
                table.byId(table.id, 1)
            }

            val expected =
                if (jvmZone == JVM_ZONE_WEST) SKIPPED_HOUR_WALL_CLOCK.plusHours(1) else SKIPPED_HOUR_WALL_CLOCK
            assertEquals(expected, row[table.local])
            assertEquals(expected.utcEpochMicros(), vendor.epochMicros(api, table.tableName, "local", 1))
        }

    @ParameterizedTest(name = "{0}, JVM {1}")
    @MethodSource("matrix")
    fun databaseDefaultsReadAsTheCurrentTime(vendor: OutsideUtcVendor, jvmZone: String) = withApi(vendor, jvmZone) { api ->
        val table = DefaultColumns()
        val row = transaction(api) {
            SchemaUtils.create(table)
            table.insert { it[id] = 1 }
            table.byId(table.id, 1)
        }

        assertCurrent(row[table.library].toInstant())
        assertCurrent(row[table.exposed])
        assertCurrentUtcWallClock(row[table.local])
        assertCurrent(vendor.epochMicrosInstant(api, table.tableName, "library", 1))
        assertCurrent(vendor.epochMicrosInstant(api, table.tableName, "exposed", 1))
    }

    @ParameterizedTest(name = "{0}, JVM {1}")
    @MethodSource("matrix")
    fun auditableTimestampsFollowInsertAndUpdate(vendor: OutsideUtcVendor, jvmZone: String) = withApi(vendor, jvmZone) { api ->
        val table = AuditedRows()
        val inserted = transaction(api) {
            SchemaUtils.create(table)
            table.insert {
                it[id] = 1
                it[note] = "inserted"
            }
            table.byId(table.id, 1)
        }
        val updated = transaction(api) {
            table.update({ table.id eq 1 }) { it[note] = "updated" }
            table.byId(table.id, 1)
        }

        assertCurrent(inserted[table.createdAt].toInstant())
        assertCurrent(inserted[table.updatedAt].toInstant())
        assertCurrent(updated[table.updatedAt].toInstant())
        assertTrue(updated[table.updatedAt].toInstant() > inserted[table.updatedAt].toInstant())
        assertEquals(inserted[table.createdAt], updated[table.createdAt])
        assertCurrent(vendor.epochMicrosInstant(api, table.tableName, "created_at", 1))
        assertCurrent(vendor.epochMicrosInstant(api, table.tableName, "updated_at", 1))
    }

    @ParameterizedTest(name = "JVM {0}")
    @ValueSource(strings = [JVM_ZONE_WEST, JVM_ZONE_EAST])
    fun postgresTimestampWithTimeZoneKeepsTheInstant(jvmZone: String) = withApi(OutsideUtcVendor.POSTGRESQL, jvmZone) { api ->
        val table = OffsetColumns()
        val written = REPEATED_HOUR.atOffset(ZoneOffset.ofHoursMinutes(5, 45))
        val row = transaction(api) {
            SchemaUtils.create(table)
            table.insert {
                it[id] = 1
                it[value] = written
            }
            table.byId(table.id, 1)
        }

        assertEquals(REPEATED_HOUR, row[table.value].toInstant())
        assertEquals(REPEATED_HOUR.epochMicros(), OutsideUtcVendor.POSTGRESQL.epochMicros(api, table.tableName, "value", 1))
        assertCurrent(row[table.generated].toInstant())
    }

    @ParameterizedTest(name = "{0}, JVM {1}")
    @MethodSource("matrix")
    fun sqlMigrationValuesAgreeWithEveryColumnType(vendor: OutsideUtcVendor, jvmZone: String) = withApi(vendor, jvmZone) { api ->
        api.migrate("tz_matrix") { sql("migrations/timezone/{vendor}") }
        val library = SqlRowsAsLibrary()
        val exposed = SqlRowsAsExposed()
        val local = SqlRowsAsLocal()

        transaction(api) {
            library.insert {
                it[id] = 3
                it[plain] = REPEATED_HOUR.atJvmOffset()
                it[withZone] = REPEATED_HOUR.atJvmOffset()
            }
            exposed.insert {
                it[id] = 4
                it[plain] = REGULAR
                it[withZone] = REGULAR
            }
        }

        transaction(api) {
            val generated = library.byId(library.id, 1)
            assertCurrent(generated[library.plain].toInstant())
            assertCurrent(generated[library.withZone].toInstant())
            val generatedByExposed = exposed.byId(exposed.id, 1)
            assertCurrent(generatedByExposed[exposed.plain])
            assertCurrent(generatedByExposed[exposed.withZone])
            val generatedWallClock = local.byId(local.id, 1)
            assertCurrentUtcWallClock(generatedWallClock[local.plain])
            assertCurrentUtcWallClock(generatedWallClock[local.withZone])

            val literal = library.byId(library.id, 2)
            assertEquals(REGULAR, literal[library.plain].toInstant())
            assertEquals(REGULAR, literal[library.withZone].toInstant())
            val literalByExposed = exposed.byId(exposed.id, 2)
            assertEquals(REGULAR, literalByExposed[exposed.plain])
            assertEquals(REGULAR, literalByExposed[exposed.withZone])
            val literalWallClock = local.byId(local.id, 2)
            assertEquals(WALL_CLOCK, literalWallClock[local.plain])
            assertEquals(WALL_CLOCK, literalWallClock[local.withZone])

            val writtenByLibrary = library.byId(library.id, 3)
            assertEquals(REPEATED_HOUR, writtenByLibrary[library.plain].toInstant())
            assertEquals(REPEATED_HOUR, writtenByLibrary[library.withZone].toInstant())
            val writtenByExposed = exposed.byId(exposed.id, 4)
            assertEquals(REGULAR, writtenByExposed[exposed.plain])
            assertEquals(REGULAR, writtenByExposed[exposed.withZone])
        }

        for (column in listOf("plain", "with_zone")) {
            assertCurrent(vendor.epochMicrosInstant(api, "tz_sql", column, 1))
            assertEquals(REGULAR.epochMicros(), vendor.epochMicros(api, "tz_sql", column, 2))
            assertEquals(REPEATED_HOUR.epochMicros(), vendor.epochMicros(api, "tz_sql", column, 3))
            assertEquals(REGULAR.epochMicros(), vendor.epochMicros(api, "tz_sql", column, 4))
        }
    }

    private fun withApi(vendor: OutsideUtcVendor, jvmZone: String, block: suspend (DatabaseApi) -> Unit) = withJvmZone(jvmZone) {
        val api = vendor.container().toDatabaseApi(fresh = true)
        try {
            block(api)
        } finally {
            api.shutdown()
        }
    }

    private suspend fun <T> transaction(api: DatabaseApi, block: suspend R2dbcTransaction.() -> T): T =
        suspendTransaction(api.database) { block() }

    private suspend fun Table.byId(idColumn: Column<Int>, id: Int): ResultRow =
        selectAll().where { idColumn eq id }.single()

    private fun assertCurrent(instant: Instant) {
        val drift = Duration.between(Instant.now(), instant).abs()
        assertTrue(drift < MAX_DRIFT, "$instant is $drift away from now")
    }

    private fun assertCurrentUtcWallClock(wallClock: LocalDateTime) = assertCurrent(wallClock.toInstant(ZoneOffset.UTC))

    private fun Instant.atJvmOffset(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneId.systemDefault())

    private fun Instant.atJvmZone(): ZonedDateTime = ZonedDateTime.ofInstant(this, ZoneId.systemDefault())

    private fun Instant.epochMicros(): Long = ChronoUnit.MICROS.between(Instant.EPOCH, this)

    private fun LocalDateTime.utcEpochMicros(): Long = toInstant(ZoneOffset.UTC).epochMicros()

    companion object {
        const val JVM_ZONE_WEST = "America/New_York"
        const val JVM_ZONE_EAST = "Asia/Kolkata"

        /**
         * An instant with microseconds outside any DST transition.
         */
        val REGULAR: Instant = Instant.parse("2030-06-01T12:00:00.123456Z")

        /**
         * 01:30:00.654321 EST, the second 01:30 of the DST fall-back on 2030-11-03 in [JVM_ZONE_WEST].
         */
        val REPEATED_HOUR: Instant = Instant.parse("2030-11-03T06:30:00.654321Z")

        /**
         * [REGULAR] as UTC wall clock.
         */
        val WALL_CLOCK: LocalDateTime = LocalDateTime.ofInstant(REGULAR, ZoneOffset.UTC)

        /**
         * 02:30:00.654321, skipped by the DST spring-forward on 2030-03-10 in [JVM_ZONE_WEST].
         */
        val SKIPPED_HOUR_WALL_CLOCK: LocalDateTime = LocalDateTime.parse("2030-03-10T02:30:00.654321")

        val MAX_DRIFT: Duration = Duration.ofMinutes(1)

        @JvmStatic
        fun matrix(): List<Arguments> = OutsideUtcVendor.entries.flatMap { vendor ->
            listOf(JVM_ZONE_WEST, JVM_ZONE_EAST).map { zone -> Arguments.of(vendor, zone) }
        }
    }
}
