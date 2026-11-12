package dev.slne.surf.database.reliability

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.database.reliability.operation.OperationKey
import dev.slne.surf.database.reliability.operation.StoredReply
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.connectionDetails
import dev.slne.surf.database.testing.toDatabaseApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Testcontainers(disabledWithoutDocker = true)
class ReliabilitySchemaIT {

    @ParameterizedTest
    @EnumSource(ReliabilityVendor::class)
    fun migrationKeepsItsOwnHistoryAndIsRepeatable(vendor: ReliabilityVendor) = vendor.withTwoProcesses { first, second ->
        // withTwoProcesses already migrated through the first process.
        val again = second.migrateReliabilitySchema()

        assertEquals(0, again.migrationsExecuted)
        assertEquals(
            1,
            vendor.queryLong(first, "SELECT COUNT(*) FROM flyway_schema_history_surf_reliability WHERE version = '1'")
        )
        assertEquals(0, vendor.queryLong(first, "SELECT COUNT(*) FROM surf_atomic_operations"))
    }

    @Test
    fun postgresSchemasOfOneDatabaseEachGetTheirOwnTablesAndHistory() = runBlocking {
        val container = DatabaseContainers.postgres()
        val schemas = List(2) { container.connectionDetails(fresh = true) }
        val apis = schemas.map { it.toDatabaseApi() }
        try {
            for (api in apis) {
                assertEquals(1, api.migrateReliabilitySchema().migrationsExecuted)
            }
            AtomicOperations(apis[0]).execute(OperationKey("billing", "invoices.Create", "message-1"), byteArrayOf(1)) {
                StoredReply("text/plain", ByteArray(0))
            }

            for (details in schemas) {
                val schema = details.schema
                assertEquals(
                    RELIABILITY_TABLES.size.toLong(),
                    ReliabilityVendor.POSTGRESQL.queryLong(
                        apis[0],
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$schema' " +
                                "AND table_name IN (${RELIABILITY_TABLES.joinToString { "'$it'" }})"
                    ),
                    "reliability tables in $schema"
                )
                assertEquals(
                    1,
                    ReliabilityVendor.POSTGRESQL.queryLong(
                        apis[0],
                        "SELECT COUNT(*) FROM \"$schema\".flyway_schema_history_surf_reliability WHERE version = '1'"
                    )
                )
            }
            val (owning, other) = schemas.map { it.schema }
            assertEquals(1, operationRows(apis[0], owning))
            assertEquals(0, operationRows(apis[0], other))
        } finally {
            apis.forEach { it.shutdown() }
        }
    }

    private fun operationRows(api: DatabaseApi, schema: String): Long =
        ReliabilityVendor.POSTGRESQL.queryLong(api, "SELECT COUNT(*) FROM \"$schema\".surf_atomic_operations")

    @Test
    fun mariaDbTablesOnAnotherEngineFailClearly() = runBlocking {
        val api = DatabaseContainers.mariadb().toDatabaseApi(fresh = true)
        try {
            ReliabilityVendor.MARIADB.execute(
                api,
                "CREATE TABLE surf_atomic_operations (owner VARCHAR(64), operation VARCHAR(255), " +
                        "idempotency_key VARCHAR(255), expires_at DATETIME(6)) ENGINE = MyISAM"
            )

            val failure = assertFailsWith<IllegalStateException> { api.migrateReliabilitySchema() }

            assertContains(failure.message.orEmpty(), "surf_atomic_operations: MyISAM")
        } finally {
            api.shutdown()
        }
    }

    private companion object {
        val RELIABILITY_TABLES = listOf(
            "surf_atomic_operations",
            "flyway_schema_history_surf_reliability",
        )
    }
}
