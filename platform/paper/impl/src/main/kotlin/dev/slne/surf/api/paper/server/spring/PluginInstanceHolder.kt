package dev.slne.surf.api.paper.server.spring

import org.bukkit.plugin.Plugin
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.util.ReflectionUtils
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Holds a plugin's instance, which Paper creates only after the plugin's bootstrap context exists.
 *
 * Bootstrap beans that need the plugin get a [Plugin] proxy that forwards to the instance once it
 * exists. The proxy is equal to the instance and has the same hash code, the way Bukkit compares
 * plugins, so listeners and tasks registered through it are cleaned up with the plugin. The full
 * context, built in the load phase, injects the instance itself under every type it has.
 */
internal class PluginInstanceHolder(private val pluginName: String) {

    @Volatile
    private var instance: Plugin? = null

    private val proxy: Plugin = Proxy.newProxyInstance(
        Plugin::class.java.classLoader,
        arrayOf(Plugin::class.java),
    ) { proxy, method, arguments -> forward(proxy, method, arguments) } as Plugin

    /**
     * Sets the instance Paper created.
     */
    fun set(plugin: Plugin) {
        instance = plugin
    }

    /**
     * A bean definition for the [Plugin] proxy, for the bootstrap context.
     */
    fun proxyDefinition(): RootBeanDefinition = RootBeanDefinition(Plugin::class.java).apply {
        setInstanceSupplier { proxy }
        destroyMethodName = ""
    }

    /**
     * A primary bean definition for [plugin], for the full context, so that it wins over the proxy
     * the bootstrap context above offers for [Plugin].
     *
     * Paper creates the instance, but the context manages it like any other singleton: it injects
     * `@Autowired` members and runs the `Aware` callbacks, `@PostConstruct` and every
     * `BeanPostProcessor` on it, and calls `@PreDestroy` and `DisposableBean.destroy` when it closes.
     * Only the inferred `close()` or `shutdown()` call is suppressed.
     */
    fun instanceDefinition(plugin: Plugin): RootBeanDefinition {
        return RootBeanDefinition(plugin.javaClass).apply {
            setInstanceSupplier { plugin }
            isPrimary = true
            destroyMethodName = ""
        }
    }

    private fun forward(proxy: Any, method: Method, arguments: Array<Any?>?): Any? {
        return when {
            ReflectionUtils.isEqualsMethod(method) -> isSamePlugin(proxy, arguments?.firstOrNull())
            ReflectionUtils.isHashCodeMethod(method) -> pluginName.hashCode()
            ReflectionUtils.isToStringMethod(method) -> "Plugin proxy of '$pluginName'"
            else -> invokeOnInstance(method, arguments)
        }
    }

    /**
     * Mirrors `PluginBase.equals`, which compares plugin names.
     */
    private fun isSamePlugin(proxy: Any, other: Any?): Boolean = when {
        other === proxy || other === instance -> true
        other !is Plugin || Proxy.isProxyClass(other.javaClass) -> false
        else -> runCatching { other.name == pluginName }.getOrDefault(false)
    }

    private fun invokeOnInstance(method: Method, arguments: Array<Any?>?): Any? {
        val target = checkNotNull(instance) {
            "The plugin instance of '$pluginName' is used before Paper created it. It exists from " +
                    "the load phase on; do not call it from a constructor or onBootstrap."
        }

        return try {
            method.invoke(target, *(arguments ?: EMPTY_ARGS))
        } catch (exception: InvocationTargetException) {
            throw exception.targetException
        }
    }

    companion object {
        private val EMPTY_ARGS = emptyArray<Any?>()
    }
}
