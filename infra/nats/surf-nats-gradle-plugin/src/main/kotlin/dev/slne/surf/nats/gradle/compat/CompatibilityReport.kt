package dev.slne.surf.nats.gradle.compat

/**
 * Result of [CompatibilityChecker.check]: [violations] fail the check, [warnings] are wire-compatible changes that
 * still affect Kotlin callers and are only reported.
 */
class CompatibilityReport(val violations: List<CompatibilityFinding>, val warnings: List<CompatibilityFinding>)
