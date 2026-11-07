package dev.slne.surf.database.reliability.internal

import dev.slne.surf.database.config.DatabaseType
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption

/**
 * `FOR UPDATE SKIP LOCKED` for this vendor. Exposed has no MariaDB variant; the MySQL clause renders the same SQL.
 */
internal fun DatabaseType.forUpdateSkipLocked(): ForUpdateOption = when (this) {
    DatabaseType.MARIADB -> ForUpdateOption.MySQL.ForUpdate(ForUpdateOption.MySQL.MODE.SKIP_LOCKED)
    DatabaseType.POSTGRESQL -> ForUpdateOption.PostgreSQL.ForUpdate(ForUpdateOption.PostgreSQL.MODE.SKIP_LOCKED)
}
