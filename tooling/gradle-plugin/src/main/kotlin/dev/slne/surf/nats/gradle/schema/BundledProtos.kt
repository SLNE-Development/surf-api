package dev.slne.surf.nats.gradle.schema

import java.io.InputStream

/**
 * Provides the surf protos bundled at the plugin jar root, such as `surf/nats/v1/options.proto`.
 *
 * Sources may import them without declaring a dependency, and must not contain files with the same paths.
 * wire-schema bundles the Google well-known types itself.
 */
object BundledProtos {
    /**
     * The relative proto-path root under which the schema loader serves the bundled protos. It is the base of their
     * locations in errors.
     */
    const val ROOT = "surf-nats-plugin"

    /**
     * The paths of the bundled protos relative to the jar root, sorted.
     */
    val paths: List<String> = BundledProtoPaths.PATHS

    /**
     * Opens the bundled proto at the relative [path], one of [paths]. The caller closes the stream.
     *
     * @throws IllegalStateException if the proto is missing from the plugin classpath.
     */
    fun open(path: String): InputStream {
        return BundledProtos::class.java.classLoader.getResourceAsStream(path)
            ?: error("Bundled proto $path is missing from the surf-nats plugin classpath")
    }
}
