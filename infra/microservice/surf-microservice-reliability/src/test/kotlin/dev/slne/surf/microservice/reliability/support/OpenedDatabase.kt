package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.io.r2dbc.pool.ConnectionPool

/**
 * A [DatabaseApi] together with the connection pool the test owns.
 */
class OpenedDatabase(val api: DatabaseApi, private val pool: ConnectionPool) {

    /**
     * Shuts down [api] and disposes the pool, so the database becomes unreachable through [api].
     */
    fun close() {
        try {
            api.shutdown()
        } finally {
            pool.dispose()
        }
    }

    /**
     * Runs [block] with [api] and closes this database afterwards.
     */
    suspend fun <T> use(block: suspend (DatabaseApi) -> T): T = try {
        block(api)
    } finally {
        close()
    }
}
