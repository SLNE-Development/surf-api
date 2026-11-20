package dev.slne.surf.api.spring.context

import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.config.DependencyDescriptor
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.LifecycleProcessor
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.GenericApplicationContext

/**
 * Builds the import context that sits between a plugin's context and its parent.
 *
 * The import context is refreshed while still empty, so none of its refresh callbacks ever sees an
 * export, and its lifecycle processor does nothing, so closing it never stops anything. Each export
 * is registered as a lazy, synthetic definition that carries the type, primary flag and qualifier
 * for autowiring. [ImportBeanFactory] delivers its instance from the provider's context on the first
 * real lookup, so the import context never initializes, post-processes or destroys an exported
 * instance; the providing plugin's context does all of that.
 *
 * @throws IllegalStateException if two different exports share an import name
 */
internal fun createImportContext(
    consumer: String,
    parent: ApplicationContext,
    exports: List<PluginExport>,
): GenericApplicationContext {
    val exportsByName = exports.associateBy(PluginExport::importName)
    check(exportsByName.size == exports.size) {
        val duplicates = exports.groupBy(PluginExport::importName).filterValues { it.size > 1 }.keys
        "Plugin '$consumer' imports several beans under the names $duplicates; a provider exports each bean name once"
    }

    val beanFactory = ImportBeanFactory(exportsByName)
    beanFactory.registerSingleton(
        AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME,
        InertLifecycleProcessor,
    )

    val context = GenericApplicationContext(beanFactory)
    context.parent = parent
    context.displayName = "surf import context of '$consumer'"

    context.refresh()

    for (export in exports) {
        context.registerBeanDefinition(
            export.importName,
            export.toBeanDefinition(),
        )
    }

    return context
}

/**
 * The bean factory of an import context.
 *
 * An export is a lazy singleton that [createBean] fetches from its provider instead of creating it.
 * Spring caches the result under this factory's singleton lock and applies no Aware or init
 * callback, no post-processor and no destruction to it. Type checks are answered from the export's
 * definition and never fetch.
 *
 * A programmatic lookup by type, such as `getBean(Class)`, that matches exports of two providers
 * finds no unique bean, whatever `@Primary` or `@Priority` say.
 */
private class ImportBeanFactory(
    private val exports: Map<String, PluginExport>,
) : DefaultListableBeanFactory() {

    /**
     * Returns the provider's instance of an export; any other bean is created as usual.
     *
     * A failure caches nothing, so the next lookup asks the provider again.
     *
     * @throws IllegalStateException if the provider is closed, or the instance implements `Lifecycle`,
     * `ApplicationListener` or [FactoryBean]
     */
    override fun createBean(
        beanName: String,
        mbd: RootBeanDefinition,
        args: Array<out Any?>?
    ): Any {
        val export = exports[beanName] ?: return super.createBean(beanName, mbd, args)
        val instance = export.instance()

        if (!export.ownReference) {
            checkNotSelfManaged(
                export.provider,
                export.beanName,
                instance.javaClass,
            )
        }

        check(instance !is FactoryBean<*>) {
            "Plugin '${export.provider}' hands out bean '${export.beanName}' as a FactoryBean, which cannot be imported"
        }

        return instance
    }

    override fun determinePrimaryCandidate(
        candidates: MutableMap<String, Any>,
        requiredType: Class<*>
    ): String? {
        return if (PluginExport.hasMultipleProviders(candidates.keys)) {
            null
        } else {
            super.determinePrimaryCandidate(candidates, requiredType)
        }
    }

    /**
     * Guards the cross-provider rule should this factory ever get a dependency comparator; without
     * one Spring finds no priorities here.
     */
    override fun determineHighestPriorityCandidate(
        candidates: MutableMap<String, Any>,
        requiredType: Class<*>
    ): String? {
        return if (PluginExport.hasMultipleProviders(candidates.keys)) {
            null
        } else {
            super.determineHighestPriorityCandidate(candidates, requiredType)
        }
    }
}

/**
 * The bean factory of a plugin context.
 *
 * It finds no unique candidate for a single-valued dependency that two different providers can
 * satisfy, even when one of them is `@Primary` or carries a higher `@Priority`: load order or a
 * provider's own preference must never decide which plugin a consumer talks to. Spring then fails
 * the dependency with a `NoUniqueBeanDefinitionException`, also when it is optional (`T?`,
 * `Optional<T>`, `ObjectProvider.getIfAvailable`); only `ObjectProvider.getIfUnique` returns `null`.
 * A consumer that wants one of them names it with `@Qualifier`, which Spring applies before this
 * check. Within one provider, and among the plugin's own beans, Spring's usual rules apply.
 *
 * Programmatic lookups by type reach the import context's factory, which applies the same rule.
 */
internal class PluginBeanFactory : DefaultListableBeanFactory() {

    override fun determineAutowireCandidate(
        candidates: MutableMap<String, Any>,
        descriptor: DependencyDescriptor,
    ): String? {
        return if (PluginExport.hasMultipleProviders(candidates.keys)) {
            null
        } else {
            super.determineAutowireCandidate(candidates, descriptor)
        }
    }
}

/**
 * A lifecycle processor that never starts or stops anything, for contexts that own no lifecycle.
 */
private object InertLifecycleProcessor : LifecycleProcessor {
    override fun onRefresh() {}
    override fun onClose() {}
    override fun start() {}
    override fun stop() {}
    override fun isRunning() = false
}
