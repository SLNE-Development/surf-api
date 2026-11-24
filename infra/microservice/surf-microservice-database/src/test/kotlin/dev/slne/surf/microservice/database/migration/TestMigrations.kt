package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.migration.JdbcMigration
import dev.slne.surf.database.migration.SurfMigration
import java.sql.Connection

object TestMigrations {
    class ClassMigration : JdbcMigration("1", "class migration") {
        override fun migrate(connection: Connection) = Unit
    }

    object ObjectMigration : JdbcMigration("2", "object migration") {
        override fun migrate(connection: Connection) = Unit
    }

    /**
     * The migration is the companion object, which has no public constructor of its own.
     */
    class WithCompanion {
        companion object : JdbcMigration("3", "companion migration") {
            override fun migrate(connection: Connection) = Unit
        }
    }

    /**
     * A shared base that discovery must skip: it is open and its constructor takes parameters.
     */
    open class OpenBase(version: String) : JdbcMigration(version, "open base") {
        override fun migrate(connection: Connection) = Unit
    }

    /**
     * Open, like a Java class, but instantiable without arguments, so discovery keeps it.
     */
    open class OpenWithDefaultConstructor : JdbcMigration("4", "open with default constructor") {
        override fun migrate(connection: Connection) = Unit
    }

    class NotAMigration
}

/**
 * Migrations that discovery must skip: an anonymous object and a local class cannot be instantiated
 * by name.
 */
val anonymousMigration: SurfMigration = object : JdbcMigration("98", "anonymous migration") {
    override fun migrate(connection: Connection) = Unit
}

fun localMigration(): SurfMigration {
    class LocalMigration : JdbcMigration("99", "local migration") {
        override fun migrate(connection: Connection) = Unit
    }

    return LocalMigration()
}
