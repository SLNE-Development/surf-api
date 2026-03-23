package dev.slne.surf.surfapi.shared.api.build

/**
 * DSL marker annotation to prevent scope leaking in nested builder DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class SurfBuilderDsl