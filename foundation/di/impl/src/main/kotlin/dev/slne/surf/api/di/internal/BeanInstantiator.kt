package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.definition.FactoryKind
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Creates the actual object behind a provider.
 *
 * Separate from the container so the container can be tested without classes on a classpath: a test
 * hands it definitions and an instantiator that returns prepared objects, and everything the
 * container does — ordering, resolution, scoping, lifecycle — is exercised without reflection.
 */
internal fun interface BeanInstantiator {

    /**
     * Returns the object for [provider].
     *
     * @param arguments resolved constructor or factory arguments, in declaration order
     * @param declaring the instance a factory is read from, `null` for a class provider
     */
    fun create(provider: BeanProvider, arguments: List<Any?>, declaring: Any?): Any
}

/**
 * Creates beans by reflection, loading their classes from [classLoader].
 *
 * A class is looked up without being initialised; reading its object instance or calling its
 * constructor is what initialises it. So a bean a condition rules out never runs its static
 * initialiser, and one that is built runs it at the moment it is built — which is also the moment
 * a static initialiser asking the container back for a bean becomes a cycle. The container cannot
 * prevent that from here: creating an instance has to initialise the class. What it can do is let
 * `SurfContainer` say so, which is why the host publishes the owner it is building.
 */
internal class ReflectiveBeanInstantiator(private val classLoader: ClassLoader) : BeanInstantiator {

    override fun create(provider: BeanProvider, arguments: List<Any?>, declaring: Any?): Any =
        when (provider) {
            is ClassProvider -> instantiateClass(provider, arguments)
            is FactoryProvider -> readFactory(provider, arguments, declaring)
        }

    private fun instantiateClass(provider: ClassProvider, arguments: List<Any?>): Any {
        val type = Class.forName(provider.definition.className, false, classLoader).kotlin

        // An `object` has no constructor to call; its instance is the bean.
        type.objectInstance?.let { instance ->
            check(arguments.isEmpty()) {
                "${provider.id} is an object declaration, so it cannot take constructor parameters."
            }
            return instance
        }

        val constructor = requireNotNull(type.primaryConstructor) {
            "${provider.id} has no primary constructor, so the container cannot create it. " +
                    "Give it one, or publish the value with @Bean from a bean that can be created."
        }

        constructor.isAccessible = true

        return requireNotNull(constructor.call(*arguments.toTypedArray())) {
            "${provider.id} returned null from its constructor."
        }
    }

    private fun readFactory(provider: FactoryProvider, arguments: List<Any?>, declaring: Any?): Any {
        val owner = requireNotNull(declaring) {
            "${provider.id} is a factory, so the bean declaring it has to exist first."
        }
        val type = owner::class
        val member = provider.factory.memberName

        return when (provider.factory.kind) {
            FactoryKind.PROPERTY -> {
                val property = requireNotNull(type.declaredMemberProperties.find { it.name == member }) {
                    "${provider.id} does not exist on ${type.qualifiedName}."
                }
                property.isAccessible = true
                requireNotNull(property.getter.call(owner)) { "${provider.id} returned null." }
            }

            FactoryKind.FUNCTION -> {
                val function = requireNotNull(
                    type.declaredMemberFunctions.find {
                        it.name == member && it.parameters.size == arguments.size + 1
                    }
                ) { "${provider.id} does not exist on ${type.qualifiedName} with ${arguments.size} parameters." }
                function.isAccessible = true
                requireNotNull(function.call(owner, *arguments.toTypedArray())) {
                    "${provider.id} returned null."
                }
            }
        }
    }
}
