package dev.slne.surf.api.nats

import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.SurfProperties
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.get
import kotlin.io.path.Path

/**
 * Installs [NatsPlatform] in a Spring Boot application whose context is surf-api's root context, such as a
 * microservice.
 *
 * Spring Boot runs it, through `META-INF/spring.factories`. It registers a bean factory post-processor and installs
 * while the context refreshes, so the order of the initializers does not matter: an initializer that adopts the
 * context into surf-api's root context, such as one that calls `SurfApiStandaloneBootstrap.adopt`, may run before or
 * after it. The context must be adopted and publish the property [SurfProperties.CONFIG_DIRECTORY] before the refresh
 * starts. NATS then reads `<directory>/nats/config.yml`. Nothing happens for a context that is not surf-api's root
 * context or has no such property, so other Spring Boot applications in the same JVM, such as test contexts with
 * their own directory, and platforms that install [NatsPlatform] themselves are unaffected.
 */
internal class NatsConfigDirectoryInitializer :
    ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(context: ConfigurableApplicationContext) {
        context.addBeanFactoryPostProcessor { installIfRoot(context) }
    }

    private fun installIfRoot(context: ConfigurableApplicationContext) {
        if (SurfContexts.rootOrNull !== context) return
        context.environment[SurfProperties.CONFIG_DIRECTORY]?.let { NatsPlatform.install(Path(it)) }
    }
}
