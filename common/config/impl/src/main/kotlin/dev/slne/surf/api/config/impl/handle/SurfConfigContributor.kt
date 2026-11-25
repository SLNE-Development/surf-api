package dev.slne.surf.api.config.impl.handle

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.SurfConfigContexts
import dev.slne.surf.api.config.impl.serializer.PluginConfigSerializers
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import dev.slne.surf.api.spring.context.ContextContributor
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.findAnnotatedClasses
import org.springframework.beans.factory.support.RootBeanDefinition
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
 * argument, so a bean injects `ConfigHandle<ShopConfig>`. The handle is created when the context
 * starts, which validates its declaration and its serializers, opens the file unless the
 * declaration is lazy, and closes with the context.
 *
 * The [SurfConfigSerializer] beans of the plugin are checked for duplicates once per context,
 * even when the plugin declares no [SurfConfig] class.
 *
 * The context is registered in [SurfConfigContexts] before its refresh, so static access through
 * `configHandle` and `surfConfig` works from the plugin's bean constructors on.
 */
@Component
class SurfConfigContributor : ContextContributor {

    override fun contribute(context: AnnotationConfigApplicationContext, plugin: PluginDescriptor) {
        val serializers = PluginConfigSerializers(context.beanFactory, plugin.name)
        context.registerBeanDefinition(
            PLUGIN_SERIALIZERS_BEAN,
            RootBeanDefinition(PluginConfigSerializers::class.java) { serializers },
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
        serializers: PluginConfigSerializers,
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
                file.createHandle(
                    dataDirectory,
                    context,
                    serializers.collectionFor(configClass, declaration),
                )
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
}

private const val PLUGIN_SERIALIZERS_BEAN =
    "dev.slne.surf.api.config.impl.handle.SurfConfigContributor#pluginSerializers"
