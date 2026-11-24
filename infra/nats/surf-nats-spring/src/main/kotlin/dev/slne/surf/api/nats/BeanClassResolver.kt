package dev.slne.surf.api.nats

import dev.slne.surf.api.common.util.logger
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.util.ClassUtils

/**
 * Resolves the class of the bean a definition describes, without creating the bean.
 *
 * Prefers Spring's type prediction and falls back to the definition's metadata where Spring
 * predicts nothing. A `FactoryBean` resolves to the factory's own class, not to its product,
 * because Spring injects into the factory. A class that cannot be loaded, also because a class it
 * references is missing, resolves to `null`.
 *
 * @property classLoader loads the classes named by the metadata without initializing them
 */
@JvmInline
internal value class BeanClassResolver(private val classLoader: ClassLoader) {

    /**
     * Returns the class of the bean [name] of [registry], or `null` for an abstract or
     * infrastructure definition and for a class that cannot be loaded.
     *
     * A bean built by a `@Bean` method resolves to the method's declared return type.
     */
    fun beanClassOf(registry: BeanDefinitionRegistry, name: String): Class<*>? {
        val definition = registry.getBeanDefinition(name)
        val isInfrastructure = definition.role == BeanDefinition.ROLE_INFRASTRUCTURE
        if (definition.isAbstract || isInfrastructure) return null

        if (registry !is ConfigurableListableBeanFactory) {
            return classFromMetadata(definition)
        }

        val beanClass = try {
            if (registry.isFactoryBean(name)) {
                null
            } else {
                registry.getType(name, false)
            }
        } catch (failure: BeansException) {
            return skipUnloadable(name, failure)
        } catch (failure: IllegalStateException) {
            if (failure.cause !is LinkageError) throw failure

            return skipUnloadable(name, failure)
        } catch (failure: LinkageError) {
            return skipUnloadable(name, failure)
        }

        return beanClass ?: classFromMetadata(definition)
    }

    private fun classFromMetadata(definition: BeanDefinition): Class<*>? {
        if (definition.factoryMethodName != null) {
            if (definition is AnnotatedBeanDefinition) {
                return definition.factoryMethodMetadata?.returnTypeName?.let(::loadClass)
            }

            return null
        }

        if (definition is AbstractBeanDefinition && definition.hasBeanClass()) {
            return definition.beanClass
        }

        return definition.beanClassName?.let(::loadClass)
    }

    private fun loadClass(className: String): Class<*>? {
        return try {
            ClassUtils.forName(className, classLoader)
        } catch (failure: ClassNotFoundException) {
            skipUnloadable(className, failure)
        } catch (failure: LinkageError) {
            skipUnloadable(className, failure)
        }
    }

    private fun skipUnloadable(name: String, failure: Throwable): Nothing? {
        log.atFine()
            .withCause(failure)
            .log("Skipping %s: its class cannot be loaded", name)

        return null
    }

    private companion object {
        val log = logger()
    }
}
