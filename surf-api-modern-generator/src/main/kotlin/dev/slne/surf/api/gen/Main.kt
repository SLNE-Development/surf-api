package dev.slne.surf.api.gen

import dev.slne.surf.api.gen.data.AdvancementCategory
import dev.slne.surf.api.gen.data.AdvancementRegistry
import dev.slne.surf.api.gen.data.Registries
import dev.slne.surf.api.gen.generator.Generators
import dev.slne.surf.api.gen.generator.SourceGenerator
import kotlinx.serialization.json.Json
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.*

class Main

fun main() {
    val json = Json {
        ignoreUnknownKeys = true
    }

    val registryFile = Main::class.java.getResourceAsStream("/registries/registries.json")!!
    val registries = json.decodeFromString<Registries>(registryFile.readAllBytes().decodeToString())

    val advancementRoot = resourceDirPath("/registries/advancement", Main::class.java)
    val advancementRegistry = loadAdvancements(advancementRoot)

    val generators = Generators(registries, advancementRegistry)

    val root = Path("../surf-api-core/surf-api-core-api/src/main")
    val java = root / "java"
    val kotlin = root / "kotlin"

    generate(java, kotlin, generators.coreApiGenerators)
}

private fun loadAdvancements(root: Path): AdvancementRegistry {
    val categories = root.listDirectoryEntries()
        .filter { it.isDirectory() }
        .sortedBy { it.name }
        .map { buildCategory(it) }

    return AdvancementRegistry(categories)
}

private fun buildCategory(dir: Path): AdvancementCategory {
    val entries = dir.listDirectoryEntries("*.json")
        .filter { it.isRegularFile() }
        .map { it.nameWithoutExtension }
        .filter { it.isNotBlank() && !it.startsWith(".") && !it.startsWith("_") }
        .sorted()

    val children = dir.listDirectoryEntries()
        .filter { it.isDirectory() && !it.name.startsWith(".") && !it.name.startsWith("_") }
        .sortedBy { it.name }
        .map { buildCategory(it) }

    return AdvancementCategory(
        name = dir.name,
        entries = entries,
        children = children
    )
}

private fun generate(outputJava: Path, outputKotlin: Path, generators: Array<SourceGenerator>) {
    outputJava.createDirectories()
    outputKotlin.createDirectories()

    for (generator in generators) {
        generator.writeToFile(outputJava, outputKotlin)
    }

    println("Generated ${generators.size} files to ${outputJava.toAbsolutePath()}")
}

private fun resourceDirPath(resourceDir: String, owner: Class<*>): Path {
    val url = requireNotNull(owner.getResource(resourceDir)) {
        "Resource directory '$resourceDir' not found on classpath"
    }
    val uri = url.toURI()
    return when (uri.scheme) {
        "file" -> uri.toPath()
        "jar" -> {
            val fs = try {
                FileSystems.getFileSystem(uri)
            } catch (_: FileSystemNotFoundException) {
                FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            }
            fs.getPath(resourceDir)
        }

        else -> error("Unsupported URI scheme for resources: ${uri.scheme}")
    }
}