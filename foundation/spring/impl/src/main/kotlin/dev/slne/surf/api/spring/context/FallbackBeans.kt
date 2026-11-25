package dev.slne.surf.api.spring.context

import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.support.GenericApplicationContext

/**
 * Registers [instance] under [name] as a fallback bean of [type].
 *
 * Every other candidate of [type] in the context wins an injection by type over this bean, so a
 * plugin's own bean of the same type never makes the injection ambiguous. The context never calls a
 * destroy method on [instance].
 */
fun <T : Any> GenericApplicationContext.registerFallbackInstance(
    name: String,
    type: Class<T>,
    instance: T,
) {
    val definition = RootBeanDefinition(type).apply {
        setInstanceSupplier { instance }
        isFallback = true
        destroyMethodName = ""
    }
    registerBeanDefinition(name, definition)
}
