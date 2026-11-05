package dev.slne.surf.nats.test.microservice.migration

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

/**
 * Code migration found by the surf-microservice processor; runs after the SQL migration `V1__create_chat_log.sql`.
 */
object SeedChatLogMigration : JdbcMigration("2", "seed chat log") {
    override fun migrate(connection: Connection) {
        connection.prepareStatement("INSERT INTO chat_log (message) VALUES (?)").use { statement ->
            statement.setString(1, SEED_MESSAGE)
            statement.executeUpdate()
        }
    }

    const val SEED_MESSAGE = "seeded by a code migration"
}
