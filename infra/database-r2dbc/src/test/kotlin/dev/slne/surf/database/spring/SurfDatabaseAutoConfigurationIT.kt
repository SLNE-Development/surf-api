package dev.slne.surf.database.spring

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.PoolConfig
import dev.slne.surf.database.migration.MigrationSet
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.database.migration.fixtures.plugina.V2_JdbcInsert
import dev.slne.surf.database.migration.fixtures.plugina.V3_ExposedInsert
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.connectionDetails
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import org.testcontainers.containers.JdbcDatabaseContainer
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.fail

/**
 * Starts [SurfDatabaseAutoConfiguration] against real servers.
 */
class SurfDatabaseAutoConfigurationIT {

    enum class Vendor(
        val container: () -> JdbcDatabaseContainer<*>,
        val lockTimeoutSql: String,
        val expectedLockTimeout: String,
    ) {
        MARIADB(DatabaseContainers::mariadb, "SELECT CAST(@@SESSION.innodb_lock_wait_timeout AS CHAR)", "3"),
        POSTGRESQL(DatabaseContainers::postgres, "SELECT current_setting('lock_timeout')", "2500ms"),
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `migration sets run before a bean receives the database`(vendor: Vendor) {
        start(vendor.container().connectionDetails(fresh = true)) {
            registerBean<PluginAMigrations>()
            registerBean<MigratedItems>()
        }.use { context ->
            assertEquals(listOf("jdbc", "exposed"), context.getBean<MigratedItems>().names)
        }
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `every connection waits at most the configured lock timeout`(vendor: Vendor) {
        start(vendor.container().connectionDetails(fresh = true)).use { context ->
            val lockTimeout = runBlocking {
                suspendTransaction(context.getBean<R2dbcDatabase>()) { single(vendor.lockTimeoutSql) }
            }
            assertEquals(vendor.expectedLockTimeout, lockTimeout)
        }
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `configurers customize the database`(vendor: Vendor) {
        start(vendor.container().connectionDetails(fresh = true)) {
            registerBean<SurfDatabaseConfigurer> { SurfDatabaseConfigurer { it.defaultFetchSize = 500 } }
        }.use { context ->
            assertEquals(500, context.getBean<R2dbcDatabase>().config.defaultFetchSize)
        }
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `transactions are not retried implicitly`(vendor: Vendor) {
        start(vendor.container().connectionDetails(fresh = true)).use { context ->
            assertEquals(1, context.getBean<R2dbcDatabase>().config.defaultMaxAttempts)
        }
    }

    @ParameterizedTest
    @EnumSource(Vendor::class)
    fun `a configurer can restore the implicit retries of Exposed`(vendor: Vendor) {
        start(vendor.container().connectionDetails(fresh = true)) {
            registerBean<SurfDatabaseConfigurer> {
                SurfDatabaseConfigurer { it.defaultMaxAttempts = EXPOSED_DEFAULT_ATTEMPTS }
            }
        }.use { context ->
            assertEquals(EXPOSED_DEFAULT_ATTEMPTS, context.getBean<R2dbcDatabase>().config.defaultMaxAttempts)
        }
    }

    @Test
    fun `a failed migration fails the start and releases the pool`() {
        val details = DatabaseContainers.mariadb().connectionDetails(fresh = true)

        assertFails {
            start(details) { registerBean<FailingMigrations>() }
        }

        awaitNoConnections(details)
    }

    @Test
    fun `closing the context releases the pool`() {
        val details = DatabaseContainers.mariadb().connectionDetails(fresh = true)

        start(details).close()

        awaitNoConnections(details)
    }

    private fun start(
        details: ConnectionDetails,
        configure: AnnotationConfigApplicationContext.() -> Unit = {},
    ) = AnnotationConfigApplicationContext().apply {
        val timeouts = PoolConfig.Timeouts(lockTimeoutMillis = 2_500)
        val pool = PoolConfig(sizing = PoolConfig.Sizing(initialSize = 2, maxSize = 2), timeouts = timeouts)
        registerBean<DatabaseConfig> { DatabaseConfig(credentials = details, pool = pool) }
        configure()
        register(SurfDatabaseAutoConfiguration::class.java)
        refresh()
    }

    /**
     * Waits until no connection of the MariaDB server uses the database of [details].
     */
    private fun awaitNoConnections(details: ConnectionDetails) {
        val container = DatabaseContainers.mariadb()
        repeat(POLL_ATTEMPTS) {
            val open = DriverManager.getConnection(container.jdbcUrl, "root", container.password).use { connection ->
                connection.prepareStatement("SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB = ?").use {
                    it.setString(1, details.database)
                    it.executeQuery().use { rows -> rows.next(); rows.getInt(1) }
                }
            }
            if (open == 0) return
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        fail("The pool still holds connections to ${details.database}")
    }

    class PluginAMigrations : MigrationSet {
        override val name = "plugin_a"

        override fun MigrationSpec.describe() {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
        }
    }

    class FailingMigrations : MigrationSet {
        override val name = "failing"

        override fun MigrationSpec.describe() {
            sql("migrations/failing")
        }
    }

    class MigratedItems(database: R2dbcDatabase) {
        val names: List<String> = runBlocking {
            suspendTransaction(database) {
                assertNotNull(exec("SELECT name FROM a_items ORDER BY id") { it.get(0, String::class.java)!! }).toList().requireNoNulls()
            }
        }
    }

    private companion object {
        const val EXPOSED_DEFAULT_ATTEMPTS = 3
        const val POLL_ATTEMPTS = 100
        const val POLL_INTERVAL_MILLIS = 50L

        suspend fun R2dbcTransaction.single(sql: String): String =
            assertNotNull(exec(sql) { it.get(0, String::class.java)!! }).toList().requireNoNulls().single()
    }
}
