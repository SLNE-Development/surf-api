package dev.slne.surf.api.config.impl.handle

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.serializer.ConfigSerializerRegistrar
import dev.slne.surf.api.config.serializer.SpongeConfigSerializers
import dev.slne.surf.api.spring.context.ContextContributor
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.findAnnotatedClasses
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * Registers a [ConfigHandle] for every [SurfConfig] class of a plugin in the plugin's context.
 *
 * Each handle is a singleton of type `ConfigHandle<C>` with the config class as its exact type
 * argument, so a bean injects `ConfigHandle<ShopConfig>`. The handle opens its file when the
 * context starts, after the plugin's [ConfigSerializerRegistrar] beans have registered their
 * serializers.
 *
 * The registrars run once per context even when the plugin declares no [SurfConfig] class, at the
 * latest once the context has instantiated its singletons.
 */
@Component
class SurfConfigContributor : ContextContributor {

    override fun contribute(context: AnnotationConfigApplicationContext, plugin: PluginDescriptor) {
        val serializers = SerializerRegistration(context)
        context.registerBeanDefinition(
            SERIALIZER_REGISTRATION_BEAN,
            RootBeanDefinition(SerializerRegistration::class.java) { serializers },
        )

        val configClasses = findAnnotatedClasses(context, plugin, SurfConfig::class.java)
        if (configClasses.isEmpty()) return

        val dataDirectory = checkNotNull(plugin.dataDirectory) {
            "Plugin '${plugin.name}' declares @SurfConfig classes ${configClasses.map { it.name }}, but has no data folder"
        }
        configClasses.forEach { register(context, it, dataDirectory, serializers) }
    }

    private fun register(
        context: AnnotationConfigApplicationContext,
        configClass: Class<*>,
        dataDirectory: Path,
        serializers: SerializerRegistration,
    ) {
        val declaration = checkNotNull(
            AnnotatedElementUtils.findMergedAnnotation(
                configClass,
                SurfConfig::class.java
            )
        ) {
            "${configClass.name} was found as a @SurfConfig class, but declares no @SurfConfig"
        }
        val definition = RootBeanDefinition(ConfigHandle::class.java).apply {
            setTargetType(
                ResolvableType.forClassWithGenerics(
                    ConfigHandle::class.java,
                    configClass
                )
            )
            setInstanceSupplier {
                serializers.applyOnce()
                open(configClass, dataDirectory, declaration)
            }
        }
        context.registerBeanDefinition("${configClass.name}#configHandle", definition)
    }

    @Suppress("UNCHECKED_CAST")
    private fun open(
        configClass: Class<*>,
        dataDirectory: Path,
        declaration: SurfConfig
    ): ConfigHandle<*> {
        return SpongeConfigHandle.open(configClass as Class<Any>, dataDirectory, declaration)
    }

    /**
     * Runs the [ConfigSerializerRegistrar] beans of one plugin context once, either before its first
     * handle opens or after its singletons are instantiated, whichever comes first.
     *
     * Both triggers fire while the context refreshes, on one thread.
     */
    private class SerializerRegistration(private val context: ApplicationContext) :
        SmartInitializingSingleton {
        private var applied = false

        override fun afterSingletonsInstantiated() = applyOnce()

        fun applyOnce() {
            if (applied) return
            applied = true

            val registrars = context.getBeansOfType<ConfigSerializerRegistrar>().values
            if (registrars.isEmpty()) return

            val serializers = context.getBean<SpongeConfigSerializers>()
            registrars.forEach { it.register(serializers) }
        }
    }
}

private const val SERIALIZER_REGISTRATION_BEAN =
    "dev.slne.surf.api.config.impl.handle.SurfConfigContributor#serializerRegistration"
