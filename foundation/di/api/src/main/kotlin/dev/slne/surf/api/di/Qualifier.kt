package dev.slne.surf.api.di

/**
 * Names a bean, and names which bean an injection point wants.
 *
 * Only needed once two beans provide the same type. The [name] on the declaration and the one on
 * the parameter have to match:
 *
 * ```kotlin
 * @Service @Qualifier("primary")  class PrimaryStorage : Storage
 * @Service @Qualifier("fallback") class FallbackStorage : Storage
 *
 * @Service
 * class Writer(@Qualifier("fallback") private val storage: Storage)
 * ```
 *
 * An injection point without a qualifier takes the only candidate, or the one marked [Primary] if
 * there are several. Without either, resolution fails and names the candidates — it never picks one.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier(val name: String)
