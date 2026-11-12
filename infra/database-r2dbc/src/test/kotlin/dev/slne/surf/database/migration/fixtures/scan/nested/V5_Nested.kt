package dev.slne.surf.database.migration.fixtures.scan.nested

import dev.slne.surf.database.migration.ExposedMigration
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

class V5_Nested : ExposedMigration("5", "nested") {
    override suspend fun R2dbcTransaction.migrate() {
        exec("SELECT 1")
    }
}
