package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.migration.SurfMigration
import java.lang.reflect.Modifier

/**
 * Resource in which the surf-microservice processor lists the `SurfMigration` classes of a module.
 *
 * UTF-8, one JVM binary class name per line; absent if the module has none.
 */
internal const val MIGRATIONS_RESOURCE = "META-INF/surf-microservice/migrations"

/**
 * Loads and instantiates the migrations listed in every [MIGRATIONS_RESOURCE] visible to [classLoader].
 *
 * A Kotlin `object` contributes its `INSTANCE`; any other class is created through its public no-argument
 * constructor. Names listed more than once are instantiated once.
 *
 * @throws IllegalStateException if a listed class cannot be loaded, is not a [SurfMigration] or cannot be
 * instantiated
 */
internal fun loadListedMigrations(
    classLoader: ClassLoader,
): List<SurfMigration> = readListedClassNames(classLoader)
    .map { className -> instantiateMigration(className, classLoader) }

private fun readListedClassNames(
    classLoader: ClassLoader,
): Set<String> = classLoader.getResources(MIGRATIONS_RESOURCE).asSequence()
    .flatMap { resource -> resource.readText(Charsets.UTF_8).lineSequence() }
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toCollection(LinkedHashSet())

private fun instantiateMigration(className: String, classLoader: ClassLoader): SurfMigration {
    val type = try {
        Class.forName(className, true, classLoader)
    } catch (exception: ClassNotFoundException) {
        throw IllegalStateException(
            "Migration class $className listed in $MIGRATIONS_RESOURCE was not found",
            exception
        )
    }

    check(SurfMigration::class.java.isAssignableFrom(type)) {
        "Class $className listed in $MIGRATIONS_RESOURCE does not extend ${SurfMigration::class.java.name}"
    }

    return try {
        SurfMigration::class.java.cast(objectInstance(type) ?: type.getConstructor().newInstance())
    } catch (exception: NoSuchMethodException) {
        throw IllegalStateException(
            "Migration $className must be an object or have a public no-argument constructor",
            exception
        )
    } catch (exception: ReflectiveOperationException) {
        throw IllegalStateException("Could not instantiate migration $className", exception)
    }
}

private fun objectInstance(type: Class<*>): Any? {
    val field = try {
        type.getField("INSTANCE")
    } catch (_: NoSuchFieldException) {
        return null
    }
    val isOwnInstance = Modifier.isStatic(field.modifiers) && field.type == type
    return if (isOwnInstance) field.get(null) else null
}
