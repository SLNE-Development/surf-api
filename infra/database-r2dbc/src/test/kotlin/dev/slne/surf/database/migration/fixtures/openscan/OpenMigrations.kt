package dev.slne.surf.database.migration.fixtures.openscan

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

/**
 * A shared base class whose constructor takes a parameter, so it cannot be created by itself.
 */
open class OpenBase(version: String) : JdbcMigration(version, "open base") {
    override fun migrate(connection: Connection) = Unit
}

/**
 * The final subclass of [OpenBase] that a scan finds.
 */
class V8_FromBase : OpenBase("8")

/**
 * A non-final class with a public no-argument constructor.
 */
open class OpenDefault : JdbcMigration("1", "open default") {
    override fun migrate(connection: Connection) = Unit
}
