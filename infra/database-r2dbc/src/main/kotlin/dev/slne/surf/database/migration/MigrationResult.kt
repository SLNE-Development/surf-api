package dev.slne.surf.database.migration

/**
 * Outcome of one `migrate()` run.
 *
 * @property initialVersion Schema version before the run. `null` if the plugin had no history on an empty database;
 * the baseline version if this run baselined a non-empty database.
 * @property targetVersion Version of the last migration executed in this run, or `null` if none was executed. This is
 * not the current schema version: a run with nothing to do returns `null` here.
 * @property migrationsExecuted Number of migrations executed in this run.
 */
data class MigrationResult(
    val initialVersion: String?,
    val targetVersion: String?,
    val migrationsExecuted: Int,
)
