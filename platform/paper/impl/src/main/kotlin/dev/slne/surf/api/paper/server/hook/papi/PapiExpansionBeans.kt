@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.paper.hook.papi.PapiExpansion
import dev.slne.surf.api.paper.hook.papi.PapiExpansionFallback
import dev.slne.surf.api.paper.hook.papi.Placeholder
import dev.slne.surf.api.paper.hook.papi.requirePlaceholderName
import org.bukkit.plugin.Plugin
import org.springframework.beans.factory.BeanFactoryUtils
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.findAnnotationOnBean
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils

/**
 * Registers the [PapiExpansion] beans of a plugin's context with the [SurfPaperPAPIHookImpl].
 *
 * Only the beans defined in the context itself are considered, never those of its ancestors.
 */
internal object PapiExpansionBeans {

    /**
     * Registers the expansion beans of [context], owned by [plugin], and records each definition
     * and each placeholder instance it creates in [registration] as soon as it exists.
     *
     * [registration] therefore holds everything registered or created even when a later
     * registration throws, so that [unregister] and [destroyCreatedInstances] can undo a partial
     * registration.
     *
     * @param context the plugin's full context
     * @param plugin the plugin whose metadata supplies a blank author or version
     * @param hook the hook to register with
     * @param registration receives the registered definitions and created instances
     * @throws IllegalArgumentException if an expansion is malformed or its identifier is taken
     * @throws org.springframework.beans.factory.NoUniqueBeanDefinitionException if a class listed
     * in [PapiExpansion.placeholders] matches several beans
     */
    fun register(
        context: ConfigurableApplicationContext,
        plugin: Plugin,
        hook: SurfPaperPAPIHookImpl,
        registration: PapiExpansionRegistration,
    ) {
        val beanFactory = context.beanFactory

        for ((name, bean) in context.getBeansWithAnnotation<PapiExpansion>()) {
            val annotation = beanFactory.findAnnotationOnBean<PapiExpansion>(name) ?: continue
            val definition = define(
                bean = bean,
                annotation = annotation,
                defaultAuthor = plugin.pluginMeta.authors.joinToString(", "),
                defaultVersion = plugin.pluginMeta.version,
                instanceOf = { type ->
                    placeholderInstance(beanFactory, type, registration.createdInstances)
                },
            )

            hook.register(definition)
            registration.definitions += definition
        }
    }

    /**
     * Unregisters the definitions of [registration] from [hook].
     *
     * @param hook the hook they were registered with
     * @param registration the registration [register] filled
     */
    fun unregister(hook: SurfPaperPAPIHookImpl, registration: PapiExpansionRegistration) {
        for (definition in registration.definitions) {
            hook.unregister(definition.identifier, definition.owner)
        }
    }

    /**
     * Destroys the placeholder instances [register] created for [registration].
     *
     * They are no beans of the context, so closing the context does not destroy them.
     *
     * @param beanFactory the bean factory of the context [register] was called with
     * @param registration the registration [register] filled
     */
    fun destroyCreatedInstances(
        beanFactory: AutowireCapableBeanFactory,
        registration: PapiExpansionRegistration,
    ) {
        for (instance in registration.createdInstances) {
            beanFactory.destroyBean(instance)
        }

        registration.createdInstances.clear()
    }

    /**
     * Describes the expansion that [bean] declares with [annotation], with [bean] as its owner.
     *
     * A Kotlin `object` listed in [PapiExpansion.placeholders] contributes its single instance;
     * [instanceOf] supplies the instances of all other listed classes.
     *
     * @param bean the bean annotated with [annotation]
     * @param annotation the bean's [PapiExpansion]
     * @param defaultAuthor the author used when [PapiExpansion.author] is blank
     * @param defaultVersion the version used when [PapiExpansion.version] is blank
     * @param instanceOf returns the instance of a listed class that is no Kotlin `object`
     * @return the definition
     * @throws IllegalArgumentException if the identifier or a placeholder name is malformed, two
     * placeholders share a name, or a [Placeholder] method has an invalid signature
     */
    fun define(
        bean: Any,
        annotation: PapiExpansion,
        defaultAuthor: String,
        defaultVersion: String,
        instanceOf: (Class<*>) -> Any,
    ): ExpansionDefinition {
        val identifier = annotation.identifier
        requirePlaceholderName(identifier, "Expansion identifier")

        val placeholderInstances = annotation.placeholders.map { type ->
            type.objectInstance ?: instanceOf(type.java)
        }
        val resolver = AnnotatedExpansionResolver(
            placeholders = collectPlaceholders(listOf(bean) + placeholderInstances),
            fallback = bean as? PapiExpansionFallback,
        )

        return ExpansionDefinition(
            identifier = identifier,
            author = annotation.author.ifBlank { defaultAuthor },
            version = annotation.version.ifBlank { defaultVersion },
            resolver = resolver,
            owner = bean,
        )
    }

    private fun collectPlaceholders(sources: List<Any>): Map<String, PlaceholderInvoker> {
        val invokers = HashMap<String, PlaceholderInvoker>()

        for (source in sources) {
            val type = ClassUtils.getUserClass(source)

            for (method in ReflectionUtils.getUniqueDeclaredMethods(type)) {
                if (method.isBridge) continue

                val placeholder = method.getAnnotation(Placeholder::class.java) ?: continue

                val name = placeholder.value
                requirePlaceholderName(name, "Placeholder name")
                require(name !in invokers) { "Placeholder '$name' is declared twice" }

                invokers[name] = PlaceholderInvokers.create(source, method)
            }
        }

        return invokers
    }

    private fun placeholderInstance(
        beanFactory: ConfigurableListableBeanFactory,
        type: Class<*>,
        createdInstances: MutableList<Any>,
    ): Any {
        val beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type)
        if (beanNames.isNotEmpty()) return beanFactory.getBean(type)

        val instance = beanFactory.createBean(type)
        createdInstances += instance

        return instance
    }
}

/**
 * The expansions [PapiExpansionBeans.register] registered for one plugin and the placeholder
 * instances it created for them; touched only on the main thread.
 */
internal class PapiExpansionRegistration {

    /**
     * The registered definitions, in registration order.
     */
    val definitions = mutableListOf<ExpansionDefinition>()

    /**
     * The placeholder instances created through the context's bean factory instead of taken from
     * its beans.
     */
    val createdInstances = mutableListOf<Any>()
}
