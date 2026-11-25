package dev.slne.surf.api.config.impl.handle

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.SurfConfigContexts
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
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.event.ContextClosedEvent
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
 * serializers, and closes with the context.
 *
 * The context is registered in [SurfConfigContexts] before its refresh, so static access through
 * `configHandle` and `surfConfig` works from the plugin's bean constructors on.
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
            "Plugin '${plugin.name}' declares @SurfConfig classes " +
                "${configClasses.map { it.name }}, but has no data folder"
        }

        for (configClass in configClasses) {
            register(context, configClass, dataDirectory, serializers)
        }

        trackForStaticAccess(context, plugin.classLoader)
    }

    private fun register(
        context: AnnotationConfigApplicationContext,
        configClass: Class<*>,
        dataDirectory: Path,
        serializers: SerializerRegistration,
    ) {
        val declaration = AnnotatedElementUtils.findMergedAnnotation(
            configClass,
            SurfConfig::class.java,
        )
        checkNotNull(declaration) {
            "${configClass.name} was found as a @SurfConfig class, but declares no @SurfConfig"
        }

        @Suppress("UNCHECKED_CAST")
        val file = SurfConfigFile(configClass as Class<Any>, declaration)
        val handleType = ResolvableType.forClassWithGenerics(ConfigHandle::class.java, configClass)
        val definition = RootBeanDefinition(ConfigHandle::class.java).apply {
            setTargetType(handleType)
            setInstanceSupplier {
                serializers.applyOnce()
                file.open(dataDirectory, context)
            }
        }
        context.registerBeanDefinition(SurfConfigContexts.handleBeanName(configClass), definition)
    }

    private fun trackForStaticAccess(
        context: AnnotationConfigApplicationContext,
        classLoader: ClassLoader,
    ) {
        SurfConfigContexts.register(classLoader, context)
        context.addApplicationListener(
            ApplicationListener<ContextClosedEvent> { event ->
                if (event.applicationContext === context) {
                    SurfConfigContexts.unregister(context)
                }
            },
        )
    }

    /**
     * Runs the [ConfigSerializerRegistrar] beans of one plugin context once, either before its
     * first handle opens or after its singletons are instantiated, whichever comes first.
     *
     * Both triggers fire while the context refreshes, on one thread.
     */
    private class SerializerRegistration(private val context: ApplicationContext) :
        SmartInitializingSingleton {
        private var isApplied = false

        override fun afterSingletonsInstantiated() {
            applyOnce()
        }

        fun applyOnce() {
            if (isApplied) return

            isApplied = true

            val registrars = context.getBeansOfType<ConfigSerializerRegistrar>().values
            if (registrars.isEmpty()) return

            val serializers = context.getBean<SpongeConfigSerializers>()

            for (registrar in registrars) {
                registrar.register(serializers)
            }
        }
    }
}

private const val SERIALIZER_REGISTRATION_BEAN =
    "dev.slne.surf.api.config.impl.handle.SurfConfigContributor#serializerRegistration"
