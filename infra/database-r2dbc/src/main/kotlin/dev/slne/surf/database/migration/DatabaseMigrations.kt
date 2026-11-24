package dev.slne.surf.database.migration

import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.nio.file.Path

/**
 * Writes the statements that bring this database in line with [tables] to
 * `<scriptDirectory>/<scriptName>.sql`, as a starting point for the next `V<n>__*.sql` migration.
 *
 * Development helper: call it against a database that does not contain the change yet. **The
 * generated statements are not idempotent** (plain `CREATE TABLE`, `DROP` for anything that
 * differs), so edit the file before shipping it.
 *
 * @param scriptName file name without the `.sql` extension, for example `V2__items`
 * @return the written file
 * @throws IllegalArgumentException if [tables] is empty
 */
@OptIn(ExperimentalDatabaseMigrationApi::class)
suspend fun R2dbcDatabase.generateMigrationScript(
    vararg tables: Table,
    scriptDirectory: Path,
    scriptName: String,
): Path {
    return suspendTransaction(db = this) {
        MigrationUtils.generateMigrationScript(
            *tables,
            scriptDirectory = scriptDirectory.toString(),
            scriptName = scriptName,
        ).toPath()
    }
}
