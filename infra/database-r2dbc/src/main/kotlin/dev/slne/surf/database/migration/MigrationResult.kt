package dev.slne.surf.database.migration

/**
 * Outcome of a migration run.
 *
 * @property initialVersion Schema version before the run: `null` if the plugin had no history on an empty schema, the
 * baseline version if this run baselined a non-empty database.
 * @property targetVersion Version of the last migration executed during the run, or `null` if no migration was
 * executed. It is not the current schema version: a run with nothing to do has [migrationsExecuted] `0` and a `null`
 * target, even if the schema is at [initialVersion].
 * @property migrationsExecuted Number of migrations executed during the run, `0` if the schema was already up to date.
 */
data class MigrationResult(
    val initialVersion: String?,
    val targetVersion: String?,
    val migrationsExecuted: Int
)
