package dev.slne.surf.api.nats

import dev.slne.surf.api.common.util.logger
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

/**
 * Makes every `@NatsService`, `@NatsTransactional` and `@NatsListener` bean definition of the context eager, whatever
 * `@Lazy` or `spring.main.lazy-initialization` says.
 *
 * [NatsPluginContext] only serves the beans that exist when the plugin is enabled. The definition is made explicitly
 * non-lazy, which Spring Boot's lazy-initialization post-processor respects. A bean's type is predicted without
 * creating it, so a bean built by a `@Bean` method is recognized by the declared return type only. A bean whose type
 * cannot be determined, because a class it needs is missing, is skipped.
 */
internal class NatsEagerComponents : BeanFactoryPostProcessor {

    private val log = logger()

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        for (name in beanFactory.beanDefinitionNames) {
            val definition = beanFactory.getBeanDefinition(name)
            if (definition.isAbstract || definition.role == BeanDefinition.ROLE_INFRASTRUCTURE) continue

            val beanType = beanClassOrNull(beanFactory, name) ?: continue
            if (NatsServiceBeans.isNatsComponent(beanType)) {
                definition.isLazyInit = false
            }
        }
    }

    private fun beanClassOrNull(
        beanFactory: ConfigurableListableBeanFactory,
        name: String,
    ): Class<*>? = try {
        beanFactory.getType(name, false)
    } catch (failure: BeansException) {
        skip(name, failure)
    } catch (failure: IllegalStateException) {
        if (failure.cause !is LinkageError) throw failure
        skip(name, failure)
    } catch (failure: LinkageError) {
        skip(name, failure)
    }

    private fun skip(name: String, failure: Throwable): Nothing? {
        log.atFine()
            .withCause(failure)
            .log("Skipping bean %s: its type cannot be determined", name)
        return null
    }
}
