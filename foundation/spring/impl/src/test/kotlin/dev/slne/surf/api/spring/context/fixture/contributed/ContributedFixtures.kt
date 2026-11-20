package dev.slne.surf.api.spring.context.fixture.contributed

/**
 * Marks the classes a test contributor looks for.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Contributed

/**
 * Found by the contributor, but not a component, so the scan must not register it.
 */
@Contributed
data class ContributedSettings(val value: Int = 1)
