package dev.slne.surf.api.nats

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.embedded.EmbeddedNats
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.isNatsTransactional
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.ClassUtils

/**
 * Reads the NATS annotations of a bean class.
 */
internal object NatsServiceBeans {

    private val log = logger()

    /**
     * Returns the generated service interfaces that the bean of class [beanClass] serves, or `null` when the class
     * carries no `@NatsService`.
     *
     * Without `value` that is every generated service interface the class implements; `value` narrows it to the
     * listed interfaces.
     *
     * @throws IllegalStateException if the class implements no generated service interface, or `value` lists a type
     * that is not a generated service interface the class implements
     */
    fun servedInterfaces(beanName: String, beanClass: Class<*>): List<Class<*>>? {
        val annotation = AnnotatedElementUtils.findMergedAnnotation(
            beanClass,
            NatsService::class.java,
        ) ?: return null

        val implemented = ClassUtils.getAllInterfacesForClassAsSet(beanClass)
            .filter(EmbeddedNats::isServiceInterface)

        val declared = annotation.value.map { it.java }.distinct()
        val served = if (declared.isEmpty()) {
            implemented
        } else {
            declared.onEach {
                requireServed(beanName, beanClass, it)
            }
        }

        check(served.isNotEmpty()) {
            "Bean '$beanName' (${beanClass.name}) is annotated with @NatsService but implements no generated NATS " +
                    "service interface"
        }

        return served
    }

    /**
     * Whether a bean of class [beanClass] takes part in NATS: it is a `@NatsService`, `@NatsTransactional` or has
     * `@NatsListener` methods. Reads the annotations only, so it never fails for a misconfigured bean.
     *
     * A class that cannot be reflected because a class it mentions is missing at runtime, such as one of a soft
     * dependency, does not use NATS. The [LinkageError] does not reach the caller.
     */
    fun isNatsComponent(beanClass: Class<*>): Boolean = try {
        AnnotatedElementUtils.hasAnnotation(beanClass, NatsService::class.java) ||
                beanClass.isNatsTransactional() ||
                hasListenerMethods(beanClass)
    } catch (failure: LinkageError) {
        log.atFine()
            .withCause(failure)
            .log("Skipping %s: it cannot be reflected", beanClass.name)
        false
    }

    /**
     * Whether [beanClass] or a superclass declares a `@NatsListener` method.
     */
    fun hasListenerMethods(beanClass: Class<*>): Boolean {
        return generateSequence(beanClass) { it.superclass }
            .takeWhile { it != Any::class.java }
            .flatMap { it.declaredMethods.asSequence() }
            .any { it.isAnnotationPresent(NatsListener::class.java) }
    }

    private fun requireServed(beanName: String, beanClass: Class<*>, type: Class<*>) {
        check(type.isInterface && EmbeddedNats.isServiceInterface(type)) {
            "@NatsService of bean '$beanName' (${beanClass.name}) lists ${type.name}, which is not a generated NATS " +
                    "service interface"
        }

        check(type.isAssignableFrom(beanClass)) {
            "@NatsService of bean '$beanName' (${beanClass.name}) lists ${type.name}, which the class does not implement"
        }
    }
}
