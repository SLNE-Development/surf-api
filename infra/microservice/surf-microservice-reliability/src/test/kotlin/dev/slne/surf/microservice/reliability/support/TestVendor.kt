package dev.slne.surf.microservice.reliability.support

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
     * [RecordingTestService]. The reliability schema is not created; the context that uses it migrates it.
     */
    fun freshDatabase(): TestDatabase {
        val container = container()
        // MariaDB: only root may create databases; the test user is granted on the default one only.
        val username = if (this == MARIADB) MARIADB_ROOT else container.username
        val namespace = "t${counter.incrementAndGet()}_${System.nanoTime()}"
        DriverManager.getConnection(container.jdbcUrl, username, container.password).use { connection ->
            connection.createStatement().use { statement ->
                when (this) {
                    POSTGRESQL -> {
                        statement.execute("CREATE SCHEMA \"$namespace\"")
                        statement.execute("CREATE TABLE \"$namespace\".test_entries (entry VARCHAR(255) NOT NULL)")
                    }

                    MARIADB -> {
                        statement.execute("CREATE DATABASE `$namespace`")
                        statement.execute("CREATE TABLE `$namespace`.test_entries (entry VARCHAR(255) NOT NULL)")
                    }
                }
            }
        }
        return TestDatabase(this, container, username, namespace)
    }

    private companion object {
        const val MARIADB_ROOT = "root"
        val counter = AtomicInteger()
    }
}
