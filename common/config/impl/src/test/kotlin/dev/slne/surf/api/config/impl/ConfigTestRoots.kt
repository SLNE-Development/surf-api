package dev.slne.surf.api.config.impl

import dev.slne.surf.api.config.impl.handle.SurfConfigContributor
import dev.slne.surf.api.config.impl.serializer.RootConfigSerializers
import dev.slne.surf.api.spring.SurfContexts
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * Starts a root context with surf-api's config beans, as the platforms scan them, and installs it.
 *
 * @param beans further root beans, such as extra default serializers
 */
internal fun startConfigRoot(vararg beans: Class<*>): AnnotationConfigApplicationContext {
    val root = AnnotationConfigApplicationContext()
    root.scan(*DEFAULT_SERIALIZER_PACKAGES)
    root.register(RootConfigSerializers::class.java, SurfConfigContributor::class.java, *beans)
    root.refresh()
    SurfContexts.install(root)

    return root
}

private val DEFAULT_SERIALIZER_PACKAGES = arrayOf(
    "dev.slne.surf.api.config.serializer",
    "dev.slne.surf.api.config.type",
)
