package dev.slne.surf.database.reliability

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.reliability.operation.OperationKey
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.connectionDetails
import dev.slne.surf.database.testing.toDatabaseApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.testcontainers.containers.JdbcDatabaseContainer
import kotlin.test.assertNotNull
import kotlin.test.fail

/**
 * A test database vendor with the admin queries the reliability tests need to simulate crashed processes.
 */
enum class ReliabilityVendor(
    private val container: () -> JdbcDatabaseContainer<*>,
    private val connectionIdSql: String,
    private val killSql: String,
    private val lockWaitersSql: String,
    private val shareLockSuffix: String,
    private val setSessionLockTimeoutSql: String,
    private val lockTimeoutSql: String,
    /**
     * [lockTimeout] after [withTwoProcesses] with a session lock timeout.
     */
    val sessionLockTimeout: String,
) {
    POSTGRESQL(
        DatabaseContainers::postgres,
        "SELECT pg_backend_pid()",
        "SELECT pg_terminate_backend(%d)",
        "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'",
        "FOR SHARE",
        "SET lock_timeout = '7s'",
        "SELECT current_setting('lock_timeout')",
        "7s",
    ),
    MARIADB(
        DatabaseContainers::mariadb,
        "SELECT CONNECTION_ID()",
        "KILL %d",
        // INNODB_TRX and INNODB_LOCK_WAITS do not list the waiting claim on MariaDB 11; the process list does.
        // The counting query itself contains the pattern, so its own connection is excluded.
        "SELECT COUNT(*) FROM information_schema.PROCESSLIST " +
                "WHERE ID <> CONNECTION_ID() AND COMMAND = 'Query' " +
                "AND INFO LIKE '%INSERT INTO surf_atomic_operations%'",
        "LOCK IN SHARE MODE",
        "SET SESSION innodb_lock_wait_timeout = 7",
        "SELECT CAST(@@SESSION.innodb_lock_wait_timeout AS CHAR)",
        "7",
    );

    /**
     * Runs [block] with two [DatabaseApi]s on the same fresh, migrated schema, standing in for two processes with
     * separate connection pools.
     *
     * With [sessionLockTimeout] set, every R2DBC connection of both processes starts with a session lock timeout of
     * [ReliabilityVendor.sessionLockTimeout] instead of the server default.
     */
    fun withTwoProcesses(
        sessionLockTimeout: Boolean = false,
        block: suspend (first: DatabaseApi, second: DatabaseApi) -> Unit,
    ) = runBlocking {
        val details = container().connectionDetails(fresh = true)
        val sessionSetupSql = setSessionLockTimeoutSql.takeIf { sessionLockTimeout }
        val first = details.toDatabaseApi(sessionSetupSql)
        val second = details.toDatabaseApi(sessionSetupSql)
        try {
            first.migrateReliabilitySchema()
            block(first, second)
        } finally {
            first.shutdown()
            second.shutdown()
        }
    }

    /**
     * Id of the connection that runs this transaction.
     */
    suspend fun R2dbcTransaction.connectionId(): Long =
        assertNotNull(exec(connectionIdSql) { (it.get(0) as Number).toLong() }).single()!!

    /**
     * Lock timeout in effect for the next statement of this transaction, as the server formats it.
     */
    suspend fun R2dbcTransaction.lockTimeout(): String =
        assertNotNull(exec(lockTimeoutSql) { it.get(0, String::class.java) }).single()!!

    /**
     * Terminates connection [id] on the server, which rolls back its open transaction like a crashed process.
     */
    fun kill(api: DatabaseApi, id: Long) {
        admin(api) { it.execute(killSql.format(id)) }
    }

    /**
     * Waits until at least [count] statements wait for a row lock.
     */
    suspend fun awaitLockWaiters(api: DatabaseApi, count: Int) {
        repeat(LOCK_POLL_ATTEMPTS) {
            val waiters = admin(api) { statement ->
                statement.executeQuery(lockWaitersSql).use { rows -> rows.next(); rows.getInt(1) }
            }
            if (waiters >= count) return
            delay(LOCK_POLL_INTERVAL_MILLIS)
        }
        fail("Expected $count lock waiters")
    }

    /**
     * Holds a share lock on the row of [key] in an open JDBC transaction until the returned handle is closed.
     *
     * The lock blocks deletes of the row but not the duplicate-key check of an insert with the same key.
     */
    fun holdShareLock(api: DatabaseApi, key: OperationKey): AutoCloseable {
        val connection = assertNotNull(api.migrationDataSource).connection
        try {
            connection.autoCommit = false
            connection.prepareStatement(
                "SELECT owner FROM surf_atomic_operations " +
                        "WHERE owner = ? AND operation = ? AND idempotency_key = ? $shareLockSuffix"
            ).use { statement ->
                statement.setString(1, key.owner)
                statement.setString(2, key.operation)
                statement.setString(3, key.idempotencyKey)
                statement.executeQuery().use { rows -> check(rows.next()) { "No row for $key" } }
            }
        } catch (e: Exception) {
            connection.close()
            throw e
        }
        return AutoCloseable {
            connection.use { it.rollback() }
        }
    }

    /**
     * Runs [sql] over the JDBC data source of [api], outside any R2DBC transaction.
     */
    fun execute(api: DatabaseApi, sql: String) {
        admin(api) { it.execute(sql) }
    }

    /**
     * Single number returned by [sql].
     */
    fun queryLong(api: DatabaseApi, sql: String): Long = admin(api) { statement ->
        statement.executeQuery(sql).use { rows -> rows.next(); rows.getLong(1) }
    }

    private fun <T> admin(api: DatabaseApi, block: (java.sql.Statement) -> T): T =
        assertNotNull(api.migrationDataSource).connection.use { connection ->
            connection.createStatement().use(block)
        }

    private companion object {
        const val LOCK_POLL_ATTEMPTS = 200
        const val LOCK_POLL_INTERVAL_MILLIS = 50L
    }
}
