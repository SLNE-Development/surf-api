package dev.slne.surf.api.nats.core

import dev.slne.surf.nats.client.NatsClientFactoryImpl
import dev.slne.surf.nats.internal.NatsClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Publishes the [NatsClientFactory] behind `NatsClient.create` as a bean of surf-api's root
 * context.
 *
 * It lives in surf-api's package so that the root scan finds it wherever surf-nats-core is on the
 * class path, including standalone processes without surf-nats-spring.
 */
@Configuration(proxyBeanMethods = false)
internal class NatsCoreBeans {

    @Bean
    fun natsClientFactory(): NatsClientFactory = NatsClientFactoryImpl()
}
