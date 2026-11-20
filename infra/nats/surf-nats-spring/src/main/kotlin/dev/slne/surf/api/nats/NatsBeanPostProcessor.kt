package dev.slne.surf.api.nats

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.isNatsTransactional
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

/**
 * Records the [NatsClient] beans of one plugin context and validates and records its `@NatsService` and
 * `@NatsListener` beans.
 *
 * Installed on the bean factory before the context refreshes, so it runs ahead of the post-processors that create
 * proxies and sees the user class of the bean. A client is handed to [NatsPluginContext.clientCreated], which
 * connects it when the context already passed its connect point. The recorded bean names are resolved again when the plugin is
 * enabled, which yields the final bean, proxies included.
 */
internal class NatsBeanPostProcessor(
    private val nats: NatsPluginContext,
    private val beanFactory: ConfigurableListableBeanFactory,
) : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is NatsClient) {
            nats.clientCreated(bean)
        }

        if (!beanFactory.containsBean(beanName)) return bean

        val beanClass = AopProxyUtils.ultimateTargetClass(bean)
        val served = NatsServiceBeans.servedInterfaces(beanName, beanClass)

        check(served != null || !beanClass.isNatsTransactional()) {
            "Bean '$beanName' (${beanClass.name}) is @NatsTransactional but not a @NatsService"
        }

        val listens = NatsServiceBeans.hasListenerMethods(beanClass)
        if (served == null && !listens) return bean

        check(beanFactory.isSingleton(beanName)) {
            "Bean '$beanName' (${beanClass.name}) uses NATS annotations and must be a singleton"
        }

        if (served != null) {
            nats.recordService(beanName, served)
        }

        if (listens) {
            nats.recordListener(beanName, beanClass)
        }

        return bean
    }
}
