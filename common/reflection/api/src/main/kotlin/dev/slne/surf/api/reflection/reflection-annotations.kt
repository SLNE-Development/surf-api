package dev.slne.surf.api.reflection

import kotlin.reflect.KClass

/**
 * Declares an interface as a reflection proxy for the class [target] or [targetName].
 *
 * Every abstract method of the interface carries exactly one of [ReflectedMethod], [ReflectedField]
 * or [ReflectedConstructor] and is bound to the matching member of the target class when the proxy
 * is created. Default methods stay as written. Instance members take the receiver as their first
 * parameter; members marked [ReflectedStatic] take none.
 *
 * Parameter and return types of a proxy method may be supertypes of the member's types, such as
 * `Any` for a class that is not visible at compile time; the proxy casts them. Calls run through a
 * generated hidden class with constant method handles and propagate the member's exceptions
 * unchanged.
 *
 * Obtain a proxy with [reflectionProxy] or inject it: every plugin context registers a lazy bean
 * for each `@ReflectionProxy` interface in the plugin's packages.
 *
 * ```kotlin
 * @ReflectionProxy(targetName = "org.spigotmc.RestartCommand")
 * interface RestartCommandProxy {
 *     @ReflectedStatic
 *     @ReflectedMethod
 *     fun restart()
 * }
 * ```
 *
 * @property target the target class; leave it at `Unit::class` when [targetName] names it
 * @property targetName the binary name of the target class, for a class that is not visible at
 * compile time; loaded through the interface's class loader
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ReflectionProxy(
    val target: KClass<*> = Unit::class,
    val targetName: String = "",
)

/**
 * Binds a proxy method to a method of the target class or its superclasses.
 *
 * The target method is selected by name and parameter count; an exact parameter-type match wins,
 * and otherwise the single method whose parameter types are compatible with the proxy method's.
 *
 * @property name the target method's name; empty for the proxy method's own name, which on a
 * property accessor is the JVM accessor name, such as `getFoo`, so set it explicitly there
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ReflectedMethod(val name: String = "")

/**
 * Binds a proxy method to a field of the target class or its superclasses.
 *
 * | [access] | instance signature | [ReflectedStatic] signature |
 * | --- | --- | --- |
 * | [FieldAccess.GET] | `(receiver): T` | `(): T` |
 * | [FieldAccess.SET] | `(receiver, value)` | `(value)` |
 *
 * Writing a `final` field is not supported: a [FieldAccess.SET] binding to one fails when the
 * proxy is created.
 *
 * @property name the field's name; empty for the proxy method's own name, which on a property
 * accessor is the JVM accessor name, such as `getFoo`, so set it explicitly there
 * @property access whether the proxy method reads or writes the field
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ReflectedField(
    val name: String = "",
    val access: FieldAccess = FieldAccess.GET,
)

/**
 * How a [ReflectedField] proxy method accesses its field.
 */
enum class FieldAccess {
    /**
     * Reads the field.
     */
    GET,

    /**
     * Writes the field.
     */
    SET,
}

/**
 * Binds a proxy method to a constructor of the target class.
 *
 * The proxy method's parameters select the constructor as for [ReflectedMethod]; it returns the
 * new instance as the target class or one of its supertypes.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ReflectedConstructor

/**
 * Marks a [ReflectedMethod] or [ReflectedField] proxy method as bound to a static member, so it
 * takes no receiver parameter.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ReflectedStatic
