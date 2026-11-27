package dev.slne.surf.api.core.server.spring

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils

/**
 * Returns the singleton beans of [contexts] whose class declares or inherits a method annotated
 * with [annotation].
 *
 * Only the beans defined in each context itself are considered, never those of its ancestors, and
 * abstract definitions are skipped. Every method of the user class counts, including methods of
 * superclasses that a subclass overrides, so the result matches a listener registry that scans the
 * whole class hierarchy. Looking a bean up creates it if it does not exist yet.
 */
@InternalSurfApi
fun beansDeclaringMethodsAnnotated(
    contexts: List<ConfigurableApplicationContext>,
    annotation: Class<out Annotation>,
): List<Any> {
    return contexts.flatMap { context ->
        beanNamesDeclaringMethodsAnnotated(context, annotation).map(context::getBean)
    }
}

/**
 * Returns the names of the singleton beans of [context] whose class declares or inherits a method
 * annotated with [annotation], by the rules of [beansDeclaringMethodsAnnotated], without creating
 * any bean.
 */
@InternalSurfApi
fun beanNamesDeclaringMethodsAnnotated(
    context: ConfigurableApplicationContext,
    annotation: Class<out Annotation>,
): List<String> {
    return context.beanDefinitionNames.filter { name ->
        context.isConcreteSingleton(name) &&
            context.getType(name)?.declaresMethodAnnotated(annotation) == true
    }
}

private fun ConfigurableApplicationContext.isConcreteSingleton(name: String): Boolean {
    return isSingleton(name) && !beanFactory.getMergedBeanDefinition(name).isAbstract
}

private fun Class<*>.declaresMethodAnnotated(annotation: Class<out Annotation>): Boolean {
    return ReflectionUtils.getAllDeclaredMethods(ClassUtils.getUserClass(this)).any {
        it.isAnnotationPresent(annotation)
    }
}
