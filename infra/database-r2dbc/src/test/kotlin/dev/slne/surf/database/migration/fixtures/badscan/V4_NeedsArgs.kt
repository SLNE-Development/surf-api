package dev.slne.surf.database.migration.fixtures.badscan

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

@Suppress("UNUSED_PARAMETER")
class V4_NeedsArgs(x: Int) : JdbcMigration("4", "needs args") {
    override fun migrate(connection: Connection) = Unit
}
