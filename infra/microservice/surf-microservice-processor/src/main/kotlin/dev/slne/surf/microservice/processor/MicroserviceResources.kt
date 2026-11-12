package dev.slne.surf.microservice.processor

/**
 * Class names and resource paths shared with the runtime and the database module.
 *
 * Each resource is UTF-8 and holds JVM binary names separated by `\n`, without a trailing line break. The readers
 * instantiate a listed class through its public static `INSTANCE` field if it is an `object`, otherwise through its
 * public no-argument constructor.
 */
internal object MicroserviceResources {
    const val MICROSERVICE_CLASS = "dev.slne.surf.microservice.api.Microservice"
    const val SURF_MIGRATION_CLASS = "dev.slne.surf.database.migration.SurfMigration"

    /**
     * Holds the binary name of the one concrete [MICROSERVICE_CLASS] subclass of the module.
     */
    const val MAIN_CLASS_PATH = "META-INF/surf-microservice/main"

    /**
     * Holds the sorted binary names of all concrete [SURF_MIGRATION_CLASS] subclasses. Absent if there are none.
     */
    const val MIGRATIONS_PATH = "META-INF/surf-microservice/migrations"
}
