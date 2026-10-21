package dev.slne.surf.database.migration

/**
 * Outcome of a migration run.
 *
 * @property initialVersion Schema version before the run, or `null` if the plugin had no applied migrations.
 * @property targetVersion Schema version after the run, or `null` if there is none.
 * @property migrationsExecuted Number of migrations executed during the run.
 */
data class MigrationResult(
    val initialVersion: String?,
    val targetVersion: String?,
    val migrationsExecuted: Int
)
