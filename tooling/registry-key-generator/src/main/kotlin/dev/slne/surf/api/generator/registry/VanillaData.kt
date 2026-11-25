package dev.slne.surf.api.generator.registry

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Reads the output directory of the vanilla data generator.
 *
 * @param directory the generator output directory, holding `reports/` and `data/`
 */
class VanillaData(private val directory: Path) {
    private val registries: Map<String, Registry> by lazy {
        json.decodeFromString((directory / "reports" / "registries.json").readText())
    }

    /**
     * Returns the entry keys of a built-in registry, such as `minecraft:stone` for
     * `minecraft:block`.
     *
     * @param registry the registry key, such as `minecraft:block`
     * @return the entry keys, including their namespace
     * @throws IllegalArgumentException if the registries report does not contain [registry]
     */
    fun registryEntries(registry: String): Set<String> {
        return requireNotNull(registries[registry]) { "The registries report lacks $registry" }
            .entries.keys
    }

    /**
     * Returns the vanilla advancement directories.
     *
     * @return one category per top-level directory, sorted by name
     */
    fun advancementCategories(): List<AdvancementCategory> {
        return subdirectories(directory / "data" / "minecraft" / "advancement").map(::readCategory)
    }

    private fun readCategory(directory: Path): AdvancementCategory {
        return AdvancementCategory(
            name = directory.name,
            entries = directory.listDirectoryEntries("*.json")
                .map { it.nameWithoutExtension }
                .filterNot { it.isHidden() }
                .sorted(),
            children = subdirectories(directory).map(::readCategory),
        )
    }

    private fun subdirectories(directory: Path): List<Path> {
        return directory.listDirectoryEntries()
            .filter { it.isDirectory() && !it.name.isHidden() }
            .sortedBy { it.name }
    }

    private fun String.isHidden() = isBlank() || startsWith(".") || startsWith("_")

    @Serializable
    private data class Registry(val entries: Map<String, JsonElement>)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * A directory of vanilla advancements.
 *
 * @property name the directory name
 * @property entries the advancement file names without extension, sorted
 * @property children the subdirectories, sorted by name
 */
data class AdvancementCategory(
    val name: String,
    val entries: List<String>,
    val children: List<AdvancementCategory>,
)
