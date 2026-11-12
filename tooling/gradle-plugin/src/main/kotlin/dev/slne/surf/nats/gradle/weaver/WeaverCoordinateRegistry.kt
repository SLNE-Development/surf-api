package dev.slne.surf.nats.gradle.weaver

import org.gradle.api.invocation.Gradle
import java.util.concurrent.ConcurrentHashMap

/**
 * Records which project publishes each weaver `group:name` in the current build.
 *
 * The map lives in the extra properties of [Gradle] and has a JDK type, so projects that load the plugin through
 * different classloaders share it. Safe for parallel project configuration.
 */
internal object WeaverCoordinateRegistry {

    const val KEY = "dev.slne.surf.nats.weaverCoordinates"

    /**
     * Claims [coordinate] for [projectPath] and returns the project that claimed it before, or `null`.
     */
    fun claim(gradle: Gradle, coordinate: String, projectPath: String): String? =
        owners(gradle).putIfAbsent(coordinate, projectPath)?.takeIf { it != projectPath }

    private fun owners(gradle: Gradle): ConcurrentHashMap<String, String> {
        val extra = gradle.extensions.extraProperties
        synchronized(extra) {
            if (!extra.has(KEY)) extra.set(KEY, ConcurrentHashMap<String, String>())
            @Suppress("UNCHECKED_CAST")
            return extra.get(KEY) as ConcurrentHashMap<String, String>
        }
    }
}
