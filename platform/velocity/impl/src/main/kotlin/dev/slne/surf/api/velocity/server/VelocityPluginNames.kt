package dev.slne.surf.api.velocity.server

import com.google.gson.JsonParser
import com.velocitypowered.api.plugin.PluginContainer
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.isRegularFile

/**
 * Names the Velocity plugin that owns a class, by the jar the class was loaded from.
 *
 * Velocity registers a plugin only after Guice created it, and a plugin creates its Redis client
 * while Guice injects it, so the registry cannot be the only source: a jar that no registered
 * plugin claims is read for its `velocity-plugin.json`.
 *
 * @param registeredPlugins the plugins Velocity has registered so far
 */
internal class VelocityPluginNames(
    private val registeredPlugins: () -> Collection<PluginContainer>,
) {

    /**
     * Returns the id of the plugin whose jar [type] was loaded from, or the simple class name when
     * no plugin jar owns it.
     */
    fun nameOf(type: Class<*>): String {
        val source = sourceOf(type) ?: return type.simpleName

        return registeredId(source) ?: descriptorId(source) ?: type.simpleName
    }

    private fun registeredId(source: Path): String? {
        return registeredPlugins()
            .firstOrNull { container ->
                container.description.source.map(::normalize)
                    .orElse(null) == source
            }
            ?.description?.id
    }

    private fun descriptorId(source: Path): String? {
        if (!source.isRegularFile()) return null

        return runCatching {
            JarFile(source.toFile()).use { jar ->
                val entry = jar.getJarEntry(DESCRIPTOR_ENTRY) ?: return@use null

                jar.getInputStream(entry).bufferedReader().use { reader ->
                    JsonParser.parseReader(reader).asJsonObject["id"]?.asString
                }
            }
        }.getOrNull()
    }

    private fun sourceOf(type: Class<*>): Path? {
        return runCatching {
            type.protectionDomain?.codeSource?.location?.toURI()?.let { normalize(Path.of(it)) }
        }.getOrNull()
    }

    private fun normalize(path: Path): Path = path.toAbsolutePath().normalize()

    private companion object {
        const val DESCRIPTOR_ENTRY = "velocity-plugin.json"
    }
}
