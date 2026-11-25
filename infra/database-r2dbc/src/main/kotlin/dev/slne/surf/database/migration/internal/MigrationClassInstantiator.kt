package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.migration.SurfMigration
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Loads [SurfMigration] classes by binary name and creates their instances.
 */
internal object MigrationClassInstantiator {

    /**
     * Instantiates exactly the classes named by [classNames], in that order, loading no other
     * class.
     *
     * Kotlin `object`s are resolved through their `INSTANCE` field, companion objects through the
     * field of their enclosing class, other classes through their public no-arg constructor.
     *
     * @param classLoader loads the classes without initializing them
     * @param classNames binary class names, such as `com.acme.Migrations$V2_Seed` for a nested
     * class
     * @throws IllegalStateException if a class cannot be loaded, is no concrete [SurfMigration],
     * has neither an `INSTANCE` field, companion field nor a public no-arg constructor, or cannot
     * be instantiated
     */
    fun instantiate(classLoader: ClassLoader, classNames: Collection<String>): List<SurfMigration> {
        return classNames.map { className ->
            val migration = instantiate(load(className, classLoader))
            checkNotNull(migration) { "$className is not a concrete SurfMigration class" }
        }
    }

    private fun load(className: String, classLoader: ClassLoader): Class<*> {
        return wrapFailure("Could not load migration class $className") {
            Class.forName(className, false, classLoader)
        }
    }

    private fun instantiate(type: Class<*>): SurfMigration? {
        if (!isConcreteMigration(type)) return null

        return wrapFailure("Could not instantiate migration class ${type.name}") {
            val instance = findObjectField(type)
            val constructor = type.constructors.find { it.parameterCount == 0 }

            when {
                instance != null -> instance.get(null) as SurfMigration
                constructor != null -> constructor.newInstance() as SurfMigration
                else -> error(
                    "Migration class ${type.name} needs a public no-arg constructor or must be an object",
                )
            }
        }
    }

    /**
     * The static field that holds the singleton of an `object`: its own `INSTANCE`, or for a
     * companion object the field of the enclosing class that is named like the companion and has
     * its type.
     */
    private fun findObjectField(type: Class<*>): Field? {
        return type.fields.find { it.name == "INSTANCE" && it.isStatic() } ?: findCompanionField(
            type,
        )
    }

    /**
     * Returns the static field of the enclosing class that holds the companion object [type], or
     * `null` if [type] is no companion object.
     *
     * Reading the enclosing class's fields resolves all their types; a [LinkageError] from a field
     * type missing at runtime also yields `null`.
     */
    private fun findCompanionField(type: Class<*>): Field? {
        return try {
            type.declaringClass?.declaredFields
                ?.find { it.isStatic() && it.name == type.simpleName && it.type == type }
                ?.also { it.isAccessible = true }
        } catch (_: LinkageError) {
            null
        }
    }

    private fun Field.isStatic() = Modifier.isStatic(modifiers)

    private fun isConcreteMigration(type: Class<*>): Boolean {
        return SurfMigration::class.java.isAssignableFrom(type) &&
            !type.isInterface &&
            !Modifier.isAbstract(type.modifiers) &&
            !type.isAnonymousClass &&
            !type.isLocalClass &&
            !type.isSynthetic
    }

    /**
     * Rethrows reflection and linkage failures from [block] as an [IllegalStateException] with
     * [message].
     */
    private inline fun <T> wrapFailure(message: String, block: () -> T): T {
        return try {
            block()
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(message, e)
        } catch (e: LinkageError) {
            throw IllegalStateException(message, e)
        }
    }
}
