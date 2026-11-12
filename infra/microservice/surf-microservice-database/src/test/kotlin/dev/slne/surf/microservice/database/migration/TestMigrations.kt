package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

object TestMigrations {
    class ClassMigration : JdbcMigration("1", "class migration") {
        override fun migrate(connection: Connection) = Unit
    }

    object ObjectMigration : JdbcMigration("2", "object migration") {
        override fun migrate(connection: Connection) = Unit
    }

    class ConstructorArgumentMigration(version: String) : JdbcMigration(version, "needs an argument") {
        override fun migrate(connection: Connection) = Unit
    }

    class FailingMigration : JdbcMigration("3", "fails on construction") {
        init {
            error("constructor failed")
        }

        override fun migrate(connection: Connection) = Unit
    }

    class NotAMigration
}
