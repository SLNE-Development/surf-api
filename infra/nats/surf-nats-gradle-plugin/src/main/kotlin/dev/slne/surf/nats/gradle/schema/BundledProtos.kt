package dev.slne.surf.nats.gradle.schema

import java.io.InputStream

/**
 * The surf protos bundled at the plugin jar root; wire-schema bundles the Google well-known types itself.
 */
object BundledProtos {
    /**
     * Relative proto-path root under which the schema loader serves the bundled protos; it is the base of their
     * locations in errors.
     */
    const val ROOT = "surf-nats-plugin"

    val paths: List<String> = BundledProtoPaths.PATHS

    /**
     * Opens the bundled proto at the relative [path] (one of [paths]).
     */
    fun open(path: String): InputStream {
        return BundledProtos::class.java.classLoader.getResourceAsStream(path)
            ?: error("Bundled proto $path is missing from the surf-nats plugin classpath")
    }
}
