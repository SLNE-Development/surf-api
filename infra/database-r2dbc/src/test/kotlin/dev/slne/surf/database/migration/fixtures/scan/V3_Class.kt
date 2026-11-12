package dev.slne.surf.database.migration.fixtures.scan

import dev.slne.surf.database.migration.ExposedMigration
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

class V3_Class : ExposedMigration("3", "class") {
    override suspend fun R2dbcTransaction.migrate() {
        exec("SELECT 1")
    }
}
