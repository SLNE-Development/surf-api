package dev.slne.surf.database.migration.fixtures.mixedscan

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

/**
 * Scanned at version 2 through its no-argument constructor; can also be listed at another version.
 */
class V2_Default(version: String = "2") : JdbcMigration(version, "default $version") {
    override fun migrate(connection: Connection) = Unit
}
