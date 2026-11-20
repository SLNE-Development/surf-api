package dev.slne.surf.api.nats

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor

/**
 * Registers a service proxy for every generated service interface that a bean of the context asks to have injected.
 *
 * [proxyDefinition] builds the definition of one proxy. A proxy is registered once per interface, under
 * [proxyBeanName], and never replaces a definition of the same name. [RequestedNatsServices] decides which
 * interfaces a bean asks for.
 *
 * Runs after the bean definitions of `@Bean` methods and imported configurations exist, because it is not ordered
 * and Spring invokes unordered registry post-processors last.
 */
internal class NatsServiceProxyRegistrar(
    classLoader: ClassLoader,
    private val proxyDefinition: (Class<*>) -> BeanDefinition,
) : BeanDefinitionRegistryPostProcessor {

    private val beanClasses = BeanClassResolver(classLoader)

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val requested = LinkedHashSet<Class<*>>()
        for (name in registry.beanDefinitionNames) {
            val beanClass = beanClasses.beanClassOf(registry, name) ?: continue
            val definition = registry.getBeanDefinition(name)
            requested += RequestedNatsServices.requestedBy(
                beanClass = beanClass,
                beanName = name,
                includeConstructors = definition.factoryMethodName == null,
            )
        }

        for (type in requested) {
            val name = proxyBeanName(type)
            if (!registry.containsBeanDefinition(name)) {
                registry.registerBeanDefinition(name, proxyDefinition(type))
            }
        }
    }

    internal companion object {

        /**
         * Returns the bean name of the service proxy of [type].
         */
        fun proxyBeanName(type: Class<*>) = "${type.name}#natsServiceProxy"
    }
}
