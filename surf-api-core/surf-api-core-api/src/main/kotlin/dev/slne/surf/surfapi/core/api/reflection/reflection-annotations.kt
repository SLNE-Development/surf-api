package dev.slne.surf.surfapi.core.api.reflection

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * Marks a proxy method as a constructor invocation for the target class.
 *
 * When applied to a method in a [SurfProxy] interface, this annotation indicates that calling
 * the method should invoke a constructor of the target class. The method's parameters must match
 * the constructor's parameter types.
 *
 * Example:
 * ```kotlin
 * @SurfProxy(qualifiedName = "com.example.TargetClass")
 * interface TargetProxy {
 *     @Constructor
 *     fun create(name: String, value: Int): Any
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Constructor

/**
 * Marks a proxy method as a field getter or setter for the target class.
 *
 * Use this annotation to access fields (including private ones) on the target class.
 * For instance fields, the first parameter must be the instance object.
 * For setters, the last parameter is the value to set.
 *
 * @param name The field name in the target class. If empty, uses the method name
 * @param type Whether this method should get ([Type.GETTER]) or set ([Type.SETTER]) the field value
 * @param overrideFinal When true, allows setting final fields using reflection. Use with caution
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
 * Overrides the name of the target method or field.
 *
 * This annotation takes precedence over names specified in other annotations like [Field] or [Static].
 * Use it when the proxy method name differs from the target member name.
 *
 * @param value The actual name of the method or field in the target class
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Name(val value: String)

/**
 * Marks a proxy method as invoking a static method or accessing a static field in the target class.
 *
 * Static methods do not require an instance parameter. When used with [Field], this accesses
 * a static field instead of an instance field.
 *
 * @param name The static member name in the target class. If empty, uses the method name
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Static(val name: String = "")

/**
 * Designates an interface as a reflection proxy for a target class.
 *
 * Apply this annotation to an interface to enable proxy creation via [SurfReflection].
 * Specify the target class either by [value] or [qualifiedName], but not both.
 *
 * Methods in the annotated interface can use [Field], [Constructor], [Static], and [Name]
 * annotations to define reflective operations on the target class.
 *
 * @param value The target class to proxy. Use `Unit::class` if providing [qualifiedName] instead
 * @param qualifiedName The fully qualified name of the target class as a string. Use this when
 *        the target class is not accessible at compile time or is in a different module
 *
 * Example:
 * ```kotlin
 * @SurfProxy(qualifiedName = "com.example.HiddenClass")
 * interface HiddenClassProxy {
 *     @Constructor
 *     fun newInstance(): Any
 *
 *     @Field(name = "value", type = Field.Type.GETTER)
 *     fun getValue(instance: Any): String
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class SurfProxy(val value: KClass<*> = Unit::class, val qualifiedName: String = "")

