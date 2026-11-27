package dev.slne.surf.api.spring

/**
 * Excludes a platform listener bean from the registration in its context's enable phase.
 *
 * Without it, surf-api registers a Paper `Listener` bean and a Velocity bean with `@Subscribe`
 * methods before the context's `onEnable` hooks run. The annotated bean stays an ordinary bean, and
 * its owner registers it with the platform when and for as long as it needs it. Annotate the
 * class, or the `@Bean` method that declares the bean.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ManualListenerRegistration
