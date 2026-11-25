package dev.slne.surf.api.core.server.impl.reflection

import dev.slne.surf.api.reflection.ReflectionProxies
import dev.slne.surf.api.reflection.ReflectionProxy
import dev.slne.surf.api.spring.context.ContextContributor
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.findAnnotatedInterfaces
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component

/**
 * Registers a bean for every [ReflectionProxy] interface in a plugin's packages.
 *
 * Each bean is lazy, so a proxy whose target class is missing on this server fails only when a
 * bean asks for it. The bean is the instance [ReflectionProxies] caches, and it is named
 * `<interface>#reflectionProxy`. Interfaces are found by reading class metadata; their target
 * classes are not loaded during registration.
 */
@Component
class ReflectionProxyContributor : ContextContributor {

    override fun contribute(context: AnnotationConfigApplicationContext, plugin: PluginDescriptor) {
        for (type in findAnnotatedInterfaces(context, plugin, ReflectionProxy::class.java)) {
            context.registerBeanDefinition(proxyBeanName(type), proxyDefinition(type))
        }
    }

    private fun proxyDefinition(type: Class<*>): RootBeanDefinition {
        @Suppress("UNCHECKED_CAST")
        val proxyType = type as Class<Any>

        return RootBeanDefinition(proxyType) { ReflectionProxies.get(proxyType) }.apply {
            isLazyInit = true
            destroyMethodName = ""
        }
    }

    internal companion object {

        /**
         * Returns the bean name of the proxy implementing [type].
         *
         * @param type the [ReflectionProxy] interface
         * @return the bean name
         */
        fun proxyBeanName(type: Class<*>): String = "${type.name}#reflectionProxy"
    }
}
