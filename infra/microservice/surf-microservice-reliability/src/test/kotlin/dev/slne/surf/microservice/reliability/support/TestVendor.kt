package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.reliability.migrateReliabilitySchema
import org.testcontainers.containers.JdbcDatabaseContainer
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicInteger

/**
 * A database vendor of the integration tests, each backed by one JVM-wide container.
 */
enum class TestVendor(private val container: () -> JdbcDatabaseContainer<*>) {
    POSTGRESQL({ Containers.postgres }),
    MARIADB({ Containers.mariadb });

    /**
     * Creates a new empty database (MariaDB) or schema (PostgreSQL) with the `test_entries` table of
     * [RecordingTestService] and, if [migrateReliability] is set, the reliability schema.
     */
    suspend fun freshDatabase(migrateReliability: Boolean = true): TestDatabase {
        val container = container()
        // MariaDB: only root may create databases; the test user is granted on the default one only.
        val username = if (this == MARIADB) MARIADB_ROOT else container.username
        val database = TestDatabase(this, container, username, createNamespace(container, username))
        database.open("setup").use { api ->
            if (migrateReliability) api.migrateReliabilitySchema()
            suspendTransaction(db = api.database) {
                exec("CREATE TABLE test_entries (entry VARCHAR(255) NOT NULL)")
            }
        }
        return database
    }

    private fun createNamespace(container: JdbcDatabaseContainer<*>, username: String): String {
        val namespace = "t${counter.incrementAndGet()}_${System.nanoTime()}"
        DriverManager.getConnection(container.jdbcUrl, username, container.password).use { connection ->
            connection.createStatement().use { statement ->
                when (this) {
                    POSTGRESQL -> statement.execute("CREATE SCHEMA \"$namespace\"")
                    MARIADB -> statement.execute("CREATE DATABASE `$namespace`")
                }
            }
        }
        return namespace
    }

    private companion object {
        const val MARIADB_ROOT = "root"
        val counter = AtomicInteger()
    }
}
