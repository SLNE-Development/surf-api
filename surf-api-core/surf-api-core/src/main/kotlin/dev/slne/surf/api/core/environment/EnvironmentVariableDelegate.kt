package dev.slne.surf.api.core.environment

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A lazy, thread-safe environment-variable property delegate.
 *
 * The variable is resolved on first access and the parsed value, including `null`, is cached for
 * all later accesses. When no explicit name is supplied, the delegated property's name is used
 * exactly as written.
 */
class EnvironmentVariableDelegate<T> internal constructor(
    private val explicitName: String?,
    private val resolver: (String) -> T
) : PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, T>> {
    /**
     * Returns an equivalent delegate that resolves the explicit environment-variable [name].
     *
     * ```kotlin
     * val databaseHost by env.required().named("DATABASE_HOST")
     * ```
     */
    fun named(name: String): EnvironmentVariableDelegate<T> {
        require(name.isNotEmpty()) { "Environment-variable names must not be empty." }
        require('\u0000' !in name && '=' !in name) {
            "Environment-variable name '$name' contains an invalid character."
        }

        return EnvironmentVariableDelegate(name, resolver)
    }

    /**
     * Captures the property name and creates the cached read-only delegate.
     */
    override fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): ReadOnlyProperty<Any?, T> {
        val variableName = explicitName ?: property.name
        val resolved = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            resolver(variableName)
        }

        return ReadOnlyProperty { _, _ -> resolved.value }
    }
}
