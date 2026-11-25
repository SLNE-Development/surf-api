package dev.slne.surf.api.config.impl.serializer

import dev.slne.surf.api.config.SurfConfig
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBean

/**
 * Builds the serializer collections of one plugin context's [SurfConfig] files.
 *
 * A file asks its own [SurfConfig.serializers] first, then the plugin's `applyToAll` serializers,
 * then the root defaults. The plugin level holds only beans the plugin context declares itself;
 * for an application hosted in the root context it is empty, because the root level already holds
 * them.
 *
 * The plugin level is resolved once, by the first file or once the context has instantiated its
 * singletons, whichever comes first, so a duplicate fails the start even without config files.
 *
 * @param beanFactory the plugin context's bean factory
 * @param pluginName names the plugin in failure messages
 */
internal class PluginConfigSerializers(
    private val beanFactory: ConfigurableListableBeanFactory,
    private val pluginName: String,
) : SmartInitializingSingleton {

    private val root by lazy { beanFactory.getBean<RootConfigSerializers>() }
    private val pluginLevel by lazy { resolvePluginLevel() }

    override fun afterSingletonsInstantiated() {
        pluginLevel
    }

    /**
     * Returns the serializers of the file [declaration] declares for [configClass].
     *
     * @throws IllegalArgumentException if a serializer the declaration names is no
     * `@SurfConfigSerializer` bean of the plugin or the root context, two serializers of one
     * level serialize the same type, or a type cannot be resolved
     */
    fun collectionFor(configClass: Class<*>, declaration: SurfConfig): TypeSerializerCollection {
        val explicitLevel = ConfigSerializerLevel(
            "@SurfConfig(serializers) of ${configClass.name}",
            declaration.serializers.map { type -> explicit(type.java, configClass) },
        )

        return root.collection(listOf(explicitLevel, pluginLevel))
    }

    private fun resolvePluginLevel(): ConfigSerializerLevel {
        if (root.isRoot(beanFactory)) return ConfigSerializerLevel.EMPTY

        return ConfigSerializerLevel(
            "plugin '$pluginName'",
            SurfConfigSerializerBeans.applyToAll(beanFactory),
        )
    }

    private fun explicit(
        serializerClass: Class<*>,
        configClass: Class<*>,
    ): ConfigSerializerRegistration {
        val registration = SurfConfigSerializerBeans.find(beanFactory, serializerClass)
            ?: SurfConfigSerializerBeans.find(root.beanFactory, serializerClass)

        return requireNotNull(registration) {
            "${configClass.name} names ${serializerClass.name} in @SurfConfig(serializers), but " +
                "it is no @SurfConfigSerializer bean of plugin '$pluginName' or of surf-api's " +
                "root context"
        }
    }
}
