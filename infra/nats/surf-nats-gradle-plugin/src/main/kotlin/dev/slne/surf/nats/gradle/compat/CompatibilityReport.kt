package dev.slne.surf.nats.gradle.compat

/**
 * Holds the result of [CompatibilityChecker.check].
 *
 * @property violations The changes that break existing callers and fail the check, in baseline order.
 * @property warnings The wire-compatible changes that still affect Kotlin callers. They are only reported.
 */
class CompatibilityReport(val violations: List<CompatibilityFinding>, val warnings: List<CompatibilityFinding>)
