package dev.slne.surf.database.config

import dev.slne.surf.api.config.SurfConfigApi
import dev.slne.surf.api.config.createSpongeYmlConfig
import org.slf4j.event.Level
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.nio.file.Path

/**
 * The connection and pool settings of a database, read from `database.yml`.
 *
 * When read with [load], every value can be overridden by a `SURF_DATABASE_*` environment variable. Overrides apply to
 * the loaded value only and are never written to the file. A [DatabaseConfig] constructed in code is used as it is,
 * without overrides.
 *
 * @property logLevel the level at which Exposed logs every statement
 */
@ConfigSerializable
data class DatabaseConfig(
    val logLevel: Level = Level.DEBUG,
    val credentials: ConnectionDetails = ConnectionDetails(),
    val pool: PoolConfig = PoolConfig(),
) {
    companion object {

        /**
         * The file the settings are read from unless a caller names another one.
         */
        const val FILE_NAME = "database.yml"

        /**
         * Reads `<directory>/<fileName>`, writing it with its defaults if it does not exist, and applies the
         * environment overrides.
         *
         * @throws IllegalArgumentException if a value is out of range after the overrides
         */
        fun load(
            configApi: SurfConfigApi,
            directory: Path,
            fileName: String = FILE_NAME
        ): DatabaseConfig {
            return configApi.createSpongeYmlConfig<DatabaseConfig>(directory, fileName)
                .withEnvironmentOverrides()
        }
    }
}
