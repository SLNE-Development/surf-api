package dev.slne.surf.api.nats

import dev.slne.surf.nats.embedded.EmbeddedNats
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.core.MethodParameter
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.util.ReflectionUtils
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.util.*

/**
 * Finds the generated service interfaces that a bean class asks Spring to inject.
 *
 * Only injection points are inspected: the constructor Spring uses to create the bean, non-static `@Autowired` and
 * `@Inject` fields and methods, and the parameters of `@Bean` methods, over the whole class hierarchy. A parameter of
 * type `ObjectProvider<T>`, `ObjectFactory<T>`, `Optional<T>` or `Provider<T>` asks for `T`. Collections are not
 * inspected, and neither is a lookup through `getBean`.
 */
internal object RequestedNatsServices {

    private val INJECT_ANNOTATIONS = listOf(
        Autowired::class.java.name,
        "jakarta.inject.Inject",
        "javax.inject.Inject",
    )

    private val PROVIDER_TYPES = setOf("jakarta.inject.Provider", "javax.inject.Provider")

    /**
     * What kotlin-reflect throws for a constructor parameter whose type is missing.
     */
    private const val KOTLIN_REFLECTION_ERROR =
        "kotlin.reflect.jvm.internal.KotlinReflectionInternalError"

    /**
     * Returns the generated service interfaces that [beanClass] asks Spring to inject.
     *
     * The constructors are the ones Spring's `AutowiredAnnotationBeanPostProcessor` selects: an `@Autowired`
     * constructor, the single constructor or the Kotlin primary constructor. The type arguments of generic members are
     * resolved against [beanClass], so a member that a generic superclass declares as `S` asks for the type
     * [beanClass] binds `S` to. A class that cannot be reflected because a type it mentions, also as a type argument,
     * is missing asks for nothing.
     *
     * @param beanName the bean's name, for Spring's constructor selection
     * @param includeConstructors `false` for the result of a factory method, whose constructors Spring does not call
     */
    fun requestedBy(
        beanClass: Class<*>,
        beanName: String = beanClass.name,
        includeConstructors: Boolean = true,
    ): Set<Class<*>> {
        val requested = LinkedHashSet<Class<*>>()

        try {
            if (includeConstructors) collectConstructorParameter(beanClass, beanName, requested)
            collectFields(beanClass, requested)
            collectMethodParameters(beanClass, requested)
        } catch (_: LinkageError) {
            return emptySet()
        } catch (_: TypeNotPresentException) {
            return emptySet()
        } catch (failure: IllegalStateException) {
            if (failure.cause !is LinkageError) throw failure
            return emptySet()
        }

        return requested
    }

    /**
     * Returns the class [type] asks Spring for: itself, or the type argument of a lazy lookup wrapper.
     */
    fun requestedType(type: ResolvableType): Class<*>? {
        val raw = type.resolve() ?: return null
        return if (isLookupWrapper(raw)) {
            type.getGeneric(0).resolve()
        } else {
            raw
        }
    }

    private fun isLookupWrapper(raw: Class<*>): Boolean {
        return raw == Optional::class.java ||
                ObjectFactory::class.java.isAssignableFrom(raw) ||
                raw.name in PROVIDER_TYPES
    }

    private fun collectConstructorParameter(
        beanClass: Class<*>,
        beanName: String,
        requested: MutableSet<Class<*>>,
    ) {
        val constructors = try {
            AutowiredAnnotationBeanPostProcessor()
                .determineCandidateConstructors(beanClass, beanName)
                ?.toList().orEmpty()
        } catch (_: BeanCreationException) {
            return
        } catch (failure: Error) {
            if (failure !is LinkageError && failure.javaClass.name != KOTLIN_REFLECTION_ERROR) throw failure
            return
        }

        for (constructor in constructors) {
            collectParameters(constructor, beanClass, requested)
        }
    }

    private fun collectFields(beanClass: Class<*>, requested: MutableSet<Class<*>>) {
        val fieldCallback = ReflectionUtils.FieldCallback { field ->
            add(ResolvableType.forField(field, beanClass), requested)
        }

        val fieldFilter = ReflectionUtils.FieldFilter { field ->
            isNotStatic(field) && isInjectionPoint(field)
        }

        ReflectionUtils.doWithFields(beanClass, fieldCallback, fieldFilter)
    }

    private fun collectMethodParameters(beanClass: Class<*>, requested: MutableSet<Class<*>>) {
        val methodCallback = ReflectionUtils.MethodCallback { method ->
            collectParameters(method, beanClass, requested)
        }

        val methodFilter = ReflectionUtils.USER_DECLARED_METHODS.and { method ->
            (isNotStatic(method) && isInjectionPoint(method)) || isBeanMethod(method)
        }

        ReflectionUtils.doWithMethods(beanClass, methodCallback, methodFilter)
    }

    private fun collectParameters(
        executable: Executable,
        beanClass: Class<*>,
        requested: MutableSet<Class<*>>
    ) {
        for (index in 0 until executable.parameterCount) {
            val parameter =
                MethodParameter.forExecutable(executable, index).withContainingClass(beanClass)
            add(ResolvableType.forMethodParameter(parameter), requested)
        }
    }

    private fun add(type: ResolvableType, requested: MutableSet<Class<*>>) {
        val requestedType = requestedType(type) ?: return
        if (requestedType.isInterface && EmbeddedNats.isServiceInterface(requestedType)) {
            requested += requestedType
        }
    }

    private fun isNotStatic(member: Member) = !Modifier.isStatic(member.modifiers)

    private fun isInjectionPoint(element: AnnotatedElement): Boolean {
        val annotations = MergedAnnotations.from(element)
        return INJECT_ANNOTATIONS.any(annotations::isPresent)
    }

    private fun isBeanMethod(element: AnnotatedElement): Boolean {
        return MergedAnnotations.from(element).isPresent(Bean::class.java)
    }
}
