package dev.slne.surf.nats.gradle.compat

/**
 * Describes one change found by [CompatibilityChecker].
 *
 * [toString] renders `location: element: message`, the line format of the report and the failure message.
 *
 * @property location `path:line:column` in the current sources, or `baseline path:line:column` for a removal.
 * @property element The fully qualified proto name of the changed declaration, such as
 * `surf.demo.v1.PlayerService.Count`.
 * @property message What changed.
 */
class CompatibilityFinding(val location: String, val element: String, val message: String) {
    override fun toString(): String = "$location: $element: $message"
}
