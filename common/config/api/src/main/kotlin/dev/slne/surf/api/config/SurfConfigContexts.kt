package dev.slne.surf.api.config

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tracks the contexts that hold the [ConfigHandle]s of [SurfConfig] classes, keyed by the class
 * loader of the plugin or application that owns them. A lookup considers the contexts whose loader
 * is the loader of the requested class or delegates to it.
 *
 * surf-api registers a context before its refresh and unregisters it when it closes. A context
 * whose refresh failed is never unregistered; it stays inactive and is skipped. Entries are weak,
 * so a registered context never keeps a plugin's class loader alive.
 *
 * A class loader is no unique key: an application shaded into a server shares surf-api's loader
 * with other plugins. A lookup therefore picks the active context that declares the handle bean
 * named by [handleBeanName]; other `ConfigHandle` beans of the same type are ignored.
 *
 * Thread-safe.
 */
@InternalSurfApi
object SurfConfigContexts {

    private val entries = CopyOnWriteArrayList<Entry>()

    /**
     * Returns the name of the bean that holds the handle of [configClass] in its plugin's context.
     *
     * @param configClass a [SurfConfig] class
     * @return the bean name
     */
    fun handleBeanName(configClass: Class<*>): String = "${configClass.name}#configHandle"

    /**
     * Registers [context], whose `@SurfConfig` classes are loaded by [classLoader].
     *
     * @param classLoader the loader of the owner's classes
     * @param context the context that declares the owner's config handles
     */
    fun register(classLoader: ClassLoader, context: ConfigurableApplicationContext) {
        entries.removeIf(Entry::isCleared)
        entries += Entry(WeakReference(classLoader), WeakReference(context))
    }

    /**
     * Removes [context]; removing a context that is not registered does nothing.
     *
     * @param context the context to remove
     */
    fun unregister(context: ConfigurableApplicationContext) {
        entries.removeIf { entry -> entry.isCleared() || entry.context.get() === context }
    }

    /**
     * Returns the handle of [configClass] together with the context that declares it.
     *
     * The context may still be refreshing; the handle is created on demand then.
     *
     * @param configClass a [SurfConfig] class
     * @return the handle and its context
     * @throws IllegalStateException if [configClass] is not annotated with [SurfConfig], or if no
     * active context declares its handle or more than one does
     */
    internal fun <C : Any> resolve(configClass: Class<C>): ResolvedHandle<C> {
        check(AnnotatedElementUtils.hasAnnotation(configClass, SurfConfig::class.java)) {
            "${configClass.name} is not annotated with @SurfConfig"
        }

        val beanName = handleBeanName(configClass)
        val declaring = entries.mapNotNull { entry -> entry.declaring(configClass, beanName) }
        check(declaring.size <= 1) {
            "${configClass.name} has a config handle in more than one context: " +
                declaring.map(ConfigurableApplicationContext::getDisplayName)
        }

        val context = declaring.singleOrNull() ?: error(
            "No running context holds the config handle of ${configClass.name}; its plugin's " +
                "context has not started yet or is closed",
        )

        @Suppress("UNCHECKED_CAST")
        val handle = context.getBean(beanName) as ConfigHandle<C>

        return ResolvedHandle(context, handle)
    }

    private class Entry(
        val classLoader: WeakReference<ClassLoader>,
        val context: WeakReference<ConfigurableApplicationContext>,
    ) {

        fun isCleared(): Boolean = classLoader.get() == null || context.get() == null

        fun declaring(configClass: Class<*>, beanName: String): ConfigurableApplicationContext? {
            if (!sees(configClass)) return null
            val context = context.get() ?: return null
            if (!context.isActive || !context.containsBeanDefinition(beanName)) return null

            return context
        }

        private fun sees(configClass: Class<*>): Boolean {
            val ownLoader = classLoader.get() ?: return false

            return generateSequence(ownLoader, ClassLoader::getParent)
                .any { loader -> loader === configClass.classLoader }
        }
    }
}

/**
 * A config handle and the context that holds it.
 */
internal class ResolvedHandle<C : Any>(
    val context: ConfigurableApplicationContext,
    val handle: ConfigHandle<C>,
)
