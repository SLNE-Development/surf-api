package dev.slne.surf.database.migration.fixtures.plugina

import dev.slne.surf.database.migration.ExposedMigration
import dev.slne.surf.database.migration.JdbcMigration
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import java.sql.Connection

object V2_JdbcInsert : JdbcMigration("2", "jdbc insert") {
    override fun migrate(connection: Connection) {
        connection.createStatement().use { it.execute("INSERT INTO a_items (id, name) VALUES (1, 'jdbc')") }
    }
}

object V3_ExposedInsert : ExposedMigration("3", "exposed insert") {
    override suspend fun R2dbcTransaction.migrate() {
        exec("INSERT INTO a_items (id, name) VALUES (2, 'exposed')")
    }
}
