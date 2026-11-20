package dev.slne.surf.api.spring

import org.springframework.stereotype.Component

/**
 * Marks a surf-api type whose implementation is a bean of surf-api's root context.
 *
 * Every plugin context descends from the root context, so a plugin bean injects the marked type as
 * a constructor parameter instead of looking it up.
 *
 * The annotation is meta-annotated with [Component] so that IDE tooling treats the type as a Spring
 * bean even when only the API artifact is on the classpath. Component scanning never registers an
 * interface, so the marked type itself does not become a bean.
 *
 * Use it on interfaces only. The Kotlin Spring compiler plugin opens every class that carries
 * [Component], meta-annotations included, together with its members.
 */
@Component
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SurfApiBean
