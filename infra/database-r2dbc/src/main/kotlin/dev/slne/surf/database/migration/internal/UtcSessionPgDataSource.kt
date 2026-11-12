package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.config.internal.SESSION_TIME_ZONE
import org.postgresql.ds.PGSimpleDataSource
import java.sql.Connection

/**
 * [PGSimpleDataSource] whose connections run with the session time zone [SESSION_TIME_ZONE].
 *
 * pgjdbc sends the JVM default zone as `TimeZone` startup parameter, which takes precedence over `-c TimeZone=...` in
 * the `options` property, so the zone is set with `SET TIME ZONE` once the connection is open. A connection whose
 * `SET` fails is closed and the failure is rethrown.
 */
internal class UtcSessionPgDataSource : PGSimpleDataSource() {

    override fun getConnection(user: String?, password: String?): Connection {
        val connection = super.getConnection(user, password)
        try {
            connection.createStatement().use { it.execute("SET TIME ZONE '$SESSION_TIME_ZONE'") }
        } catch (e: Throwable) {
            connection.close()
            throw e
        }
        return connection
    }
}
