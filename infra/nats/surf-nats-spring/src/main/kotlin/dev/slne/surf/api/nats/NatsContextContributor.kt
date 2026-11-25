package dev.slne.surf.api.nats

import dev.slne.surf.api.spring.context.ContextContributor
import dev.slne.surf.api.spring.context.ContextPhaseListener
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.embedded.EmbeddedNats
import dev.slne.surf.nats.embedded.NatsClientControl
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import kotlin.reflect.KClass

internal const val NATS_CLIENT_BEAN = "surfNatsClient"
internal const val NATS_SERVICE_PROXY_REGISTRAR_BEAN = "surfNatsServiceProxyRegistrar"
internal const val NATS_EAGER_COMPONENTS_BEAN = "surfNatsEagerComponents"
internal const val NATS_CONNECTOR_BEAN = "surfNatsConnector"
internal const val NATS_PHASE_BEAN = "surfNatsPhase"
internal const val NATS_HEALTH_BEAN = "natsHealthIndicator"
private const val HEALTH_INDICATOR_CLASS =
    "org.springframework.boot.health.contributor.HealthIndicator"

/**
 * Gives every plugin context its NATS beans.
 *
 * - A `NatsClient` named after the plugin, see [NatsClientNames]. It is a lazy fallback bean, so a
 *   plugin's own `NatsClient` bean wins, and it closes when the context closes. Everything below
 *   resolves the client by type, so the winner serves the plugin's service proxies, services and
 *   listeners and is the one that connects.
 * - A registry post-processor, [NatsServiceProxyRegistrar], that adds a lazy fallback service proxy
 *   for every generated service interface a bean of the context asks to have injected, also through
 *   the members of a `@Bean` method's result. A `@NatsService` bean of the same plugin implements
 *   the interface without being a fallback, so it wins.
 * - A bean factory post-processor, [NatsEagerComponents], that makes the plugin's `@NatsService`
 *   and `@NatsListener` bean definitions eager, so they exist when the plugin is enabled.
 * - A bean post-processor that records the context's `NatsClient` beans and validates and records
 *   the plugin's `@NatsService` and `@NatsListener` beans.
 * - A phase listener that registers the recorded beans once the plugin is enabled and drains them
 *   before it is disabled.
 * - A connector that connects the clients if the plugin uses NATS, right after the context
 *   refreshed and before any `onLoad` hook, see [NatsPluginContext].
 * - With Spring Boot's health module on the class path, a health indicator whose bean name is
 *   [NATS_HEALTH_BEAN]. Spring Boot drops the `HealthIndicator` suffix, so the health component is
 *   `nats`, and an application bean named `nats` stays possible. See [NatsHealthIndicator].
 *
 * A plugin that uses nothing of NATS never creates a client and never connects. Service proxies and
 * the client are created on first use, so the root context never connects either.
 */
@Component
internal class NatsContextContributor : ContextContributor {

    override fun contribute(context: AnnotationConfigApplicationContext, plugin: PluginDescriptor) {
        contribute(context, plugin, ::connectShared)
    }

    /**
     * Contributes the NATS beans of [plugin] to [context], opening connections through [connect].
     */
    internal fun contribute(
        context: AnnotationConfigApplicationContext,
        plugin: PluginDescriptor,
        connect: (NatsClientControl) -> Unit,
    ) {
        val nats = NatsPluginContext(context, plugin.name, connect)

        context.beanFactory.addBeanPostProcessor(NatsBeanPostProcessor(nats, context.beanFactory))
        context.registerBeanDefinition(
            NATS_CLIENT_BEAN,
            clientDefinition(nats),
        )
        context.registerBeanDefinition(
            NATS_SERVICE_PROXY_REGISTRAR_BEAN,
            serviceProxyRegistrarDefinition(context, plugin),
        )
        context.registerBeanDefinition(
            NATS_EAGER_COMPONENTS_BEAN,
            RootBeanDefinition(NatsEagerComponents::class.java) { NatsEagerComponents() },
        )
        context.registerBeanDefinition(
            NATS_PHASE_BEAN,
            phaseDefinition(nats),
        )
        context.registerBeanDefinition(
            NATS_CONNECTOR_BEAN,
            connectorDefinition(nats),
        )

        if (ClassUtils.isPresent(
                HEALTH_INDICATOR_CLASS,
                NatsContextContributor::class.java.classLoader,
            )
        ) {
            context.registerBeanDefinition(
                NATS_HEALTH_BEAN,
                natsHealthIndicatorDefinition(nats),
            )
        }
    }

    private fun clientDefinition(nats: NatsPluginContext): RootBeanDefinition {
        return RootBeanDefinition(NatsClient::class.java).apply {
            isLazyInit = true
            isFallback = true
            setInstanceSupplier { nats.createClient() }
            destroyMethodName = "close"
        }
    }

    private fun serviceProxyRegistrarDefinition(
        context: AnnotationConfigApplicationContext,
        plugin: PluginDescriptor,
    ): RootBeanDefinition {
        return RootBeanDefinition(NatsServiceProxyRegistrar::class.java) {
            NatsServiceProxyRegistrar(plugin.classLoader) { serviceProxyDefinition(context, it) }
        }
    }

    private fun phaseDefinition(nats: NatsPluginContext): RootBeanDefinition {
        return RootBeanDefinition(NatsPhaseListener::class.java) { NatsPhaseListener(nats) }
    }

    private fun connectorDefinition(nats: NatsPluginContext): RootBeanDefinition {
        return RootBeanDefinition(NatsConnector::class.java) { NatsConnector(nats) }
    }

    internal companion object {

        /**
         * Returns the definition of the service proxy of the generated service interface [type]: a
         * lazy fallback bean that creates the proxy on the context's client, resolved by type,
         * without a connection.
         */
        fun serviceProxyDefinition(
            context: AnnotationConfigApplicationContext,
            type: Class<*>,
        ): RootBeanDefinition {
            @Suppress("UNCHECKED_CAST")
            val serviceType = type.kotlin as KClass<Any>

            return RootBeanDefinition(serviceType.java) {
                EmbeddedNats.control(context.getBean<NatsClient>()).lazyService(serviceType)
            }.apply {
                isLazyInit = true
                isFallback = true
                destroyMethodName = ""
            }
        }
    }
}

/**
 * Registers the plugin's recorded NATS beans once it is enabled, and drains them before it is
 * disabled.
 *
 * The client itself closes with the context, which also covers contexts that close without their
 * disable hooks.
 */
internal class NatsPhaseListener(private val nats: NatsPluginContext) : ContextPhaseListener {

    override suspend fun afterEnable() {
        nats.registerRecorded()
    }

    override suspend fun beforeDisable() {
        nats.stopAcceptingIfCreated()
    }
}
