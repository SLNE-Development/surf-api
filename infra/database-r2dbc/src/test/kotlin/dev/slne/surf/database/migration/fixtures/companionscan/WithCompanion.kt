package dev.slne.surf.database.migration.fixtures.companionscan

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

/**
 * The migration is the companion object of the class, which has no `INSTANCE` field and no public constructor.
 */
class WithCompanion {
    companion object : JdbcMigration("6", "companion") {
        override fun migrate(connection: Connection) = Unit
    }
}

/**
 * Like [WithCompanion], but the companion is named and private.
 */
class WithNamedCompanion {
    private companion object Seed : JdbcMigration("7", "named companion") {
        override fun migrate(connection: Connection) = Unit
    }
}
