package dev.slne.surf.api.core.config.type.number

/**
 * Marks an optional numeric config value so that negative values are treated as the empty state.
 *
 * This is mainly useful for `IntOr` and `DoubleOr` values where old or user-provided
 * negative values should behave like `default` or `disabled`.
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class ViewConfig(
 *     @field:BelowZeroToEmpty
 *     val viewDistance: IntOr.Default = IntOr.Default.USE_DEFAULT
 * )
 * ```
 *
 * In this example, `view-distance: -1` is interpreted as `default`.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class BelowZeroToEmpty