package dev.slne.surf.database.reliability.internal

import dev.slne.surf.database.DatabaseApi
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Names and server checks of the library-owned reliability schema.
 */
internal object ReliabilitySchema {
    /**
     * Migration name; Flyway keeps the history in `flyway_schema_history_surf_reliability`.
     */
    const val MIGRATION_NAME = "surf_reliability"
    const val SQL_LOCATION = "db/surf-reliability/{vendor}"

    private val tables = listOf(AtomicOperationTable.tableName)

    /**
     * Result of `SELECT VERSION()`, empty if the server returns none.
     */
    suspend fun serverVersion(api: DatabaseApi): String = api.readCommittedTransaction {
        val result = exec("SELECT VERSION()") { row ->
            row.get(0, String::class.java)
        }

        result?.toList()?.singleOrNull()
    }.orEmpty()

    /**
     * MariaDB storage engine per reliability table, `null` for a missing table.
     */
    suspend fun mariaDbTableEngines(
        api: DatabaseApi
    ): Map<String, String?> = api.readCommittedTransaction {
        val placeholders = tables.joinToString { "?" }
        val result = exec(
            "SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ($placeholders)",
            tables.map { VarCharColumnType() to it }
        ) { row ->
            row.get(0, String::class.java)!! to row.get(1, String::class.java)
        }

        val found = result?.toList()
            .orEmpty()
            .filterNotNull()
            .toMap()

        tables.associateWith { found[it] }
    }
}
