package dev.slne.surf.surfapi.core.api.reflection

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * Marks a method in a [SurfProxy] as a constructor.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Constructor

/**
 * Marks a method in a [SurfProxy] as a Field.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Field(
    val name: String = "",
    val type: Type,
    val overrideFinal: Boolean = false
) {
    enum class Type {
        SETTER,
        GETTER
    }
}

/**
 * Overrides the reflection name of a method or field. This annotation overrides anything previously
 * set in other annotations.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Name(val value: String)

/**
 * Marks a method in a [SurfProxy] as a static method.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Static(val name: String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class SurfProxy(val value: KClass<*> = Unit::class, val qualifiedName: String = "")

