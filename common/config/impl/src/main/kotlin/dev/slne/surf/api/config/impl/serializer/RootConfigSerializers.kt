package dev.slne.surf.api.config.impl.serializer

import dev.slne.surf.api.config.serializer.ConfigObjectMappers
import dev.slne.surf.api.config.serializer.ConfigSerializerCollections
import dev.slne.surf.api.config.serializer.DefaultConfigSerializerSource
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBeansOfType
import org.springframework.stereotype.Component

/**
 * Builds the serializer collections of config files, ending with the root defaults: the
 * `applyToAll` serializers and the [DefaultConfigSerializerSource]s of surf-api's root context.
 *
 * The root level is read and validated again for every collection, because a default source may
 * depend on the server's phase. It is also validated once when the root context has started, so a
 * duplicate among the annotated defaults fails the start of surf-api's runtime. A duplicate that
 * involves a source's output fails only once a collection is built while the source supplies it,
 * such as the registry serializers that Paper supplies only after its bootstrap phase.
 *
 * @property beanFactory the bean factory of surf-api's root context
 */
@Component
internal class RootConfigSerializers(
    val beanFactory: ConfigurableListableBeanFactory,
) : ConfigSerializerCollections, SmartInitializingSingleton {

    override fun afterSingletonsInstantiated() {
        rootLevel()
    }

    override fun withRootDefaults(serializers: List<TypeSerializer<*>>): TypeSerializerCollection {
        val ownLevel = ConfigSerializerLevel(
            "the serializers passed to ConfigHandles.open",
            serializers.map { serializer -> ConfigSerializerRegistration.of(serializer) },
        )

        return collection(listOf(ownLevel))
    }

    /**
     * Returns a collection that asks [levels] in order, then the root defaults, then
     * Configurate's object mapper and its own defaults.
     */
    fun collection(levels: List<ConfigSerializerLevel>): TypeSerializerCollection {
        val builder = TypeSerializerCollection.defaults().childBuilder()
        for (level in levels + rootLevel()) {
            level.registerTo(builder)
        }
        builder.registerAnnotatedObjects(ConfigObjectMappers.createFactory())

        return builder.build()
    }

    /**
     * Whether [factory] is the bean factory of surf-api's root context, which is also the context
     * of an application that the root hosts.
     */
    fun isRoot(factory: ListableBeanFactory): Boolean = factory === beanFactory

    private fun rootLevel(): ConfigSerializerLevel {
        val annotated = SurfConfigSerializerBeans.applyToAll(beanFactory)
        val supplied = beanFactory.getBeansOfType<DefaultConfigSerializerSource>()
            .values
            .flatMap { source -> source.serializers() }
            .map { serializer -> ConfigSerializerRegistration.of(serializer) }

        return ConfigSerializerLevel("surf-api's root context", annotated + supplied)
    }
}
