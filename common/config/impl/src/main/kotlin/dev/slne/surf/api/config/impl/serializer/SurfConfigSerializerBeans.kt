package dev.slne.surf.api.config.impl.serializer

import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.serialize.TypeSerializer
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.findAnnotationOnBean
import org.springframework.beans.factory.getBeanNamesForAnnotation
import org.springframework.util.ClassUtils

/**
 * Reads the [SurfConfigSerializer] beans that one bean factory declares itself, never those of its
 * ancestors, so a plugin's serializers stay invisible to its import context and to other plugins.
 */
internal object SurfConfigSerializerBeans {

    /**
     * Returns the `applyToAll` serializers declared in [beanFactory].
     */
    fun applyToAll(beanFactory: ListableBeanFactory): List<ConfigSerializerRegistration> {
        return beanFactory.getBeanNamesForAnnotation<SurfConfigSerializer>()
            .filter { name -> declaration(beanFactory, name).applyToAll }
            .map { name -> registration(beanFactory, name) }
    }

    /**
     * Returns the [SurfConfigSerializer] bean of [beanFactory] whose class is exactly
     * [serializerClass], or `null` if [beanFactory] declares none; beans of subclasses do not
     * count.
     *
     * @throws IllegalArgumentException if [beanFactory] declares several
     */
    fun find(
        beanFactory: ListableBeanFactory,
        serializerClass: Class<*>,
    ): ConfigSerializerRegistration? {
        val names = beanFactory.getBeanNamesForType(serializerClass)
            .filter { name -> isBeanOfExactClass(beanFactory, name, serializerClass) }
            .filter { name ->
                beanFactory.findAnnotationOnBean<SurfConfigSerializer>(name) != null
            }
        require(names.size <= 1) {
            "Config serializer ${serializerClass.name} has several beans: $names"
        }
        val name = names.singleOrNull() ?: return null

        return registration(beanFactory, name)
    }

    private fun isBeanOfExactClass(
        beanFactory: ListableBeanFactory,
        name: String,
        serializerClass: Class<*>,
    ): Boolean {
        val beanClass = beanFactory.getType(name) ?: return false

        return ClassUtils.getUserClass(beanClass) == serializerClass
    }

    private fun registration(
        beanFactory: ListableBeanFactory,
        name: String,
    ): ConfigSerializerRegistration {
        val declaredType = declaration(beanFactory, name).type.java
        val serializer = beanFactory.getBean(name)
        require(serializer is TypeSerializer<*>) {
            "Bean '$name' of ${serializer.javaClass.name} is annotated with " +
                "@SurfConfigSerializer, but is no Configurate TypeSerializer"
        }

        return ConfigSerializerRegistration.of(
            serializer,
            declaredType.takeUnless { it == Void::class.java },
        )
    }

    private fun declaration(beanFactory: ListableBeanFactory, name: String): SurfConfigSerializer {
        return checkNotNull(
            beanFactory.findAnnotationOnBean<SurfConfigSerializer>(name),
        ) {
            "Bean '$name' was found as a @SurfConfigSerializer, but declares none"
        }
    }
}
