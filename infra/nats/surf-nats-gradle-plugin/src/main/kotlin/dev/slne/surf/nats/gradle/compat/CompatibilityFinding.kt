package dev.slne.surf.nats.gradle.compat

/**
 * One reported change; [location] is `path:line:column` in the current sources, or `baseline path:line:column` for
 * removals.
 */
class CompatibilityFinding(val location: String, val element: String, val message: String) {
    override fun toString(): String = "$location: $element: $message"
}
