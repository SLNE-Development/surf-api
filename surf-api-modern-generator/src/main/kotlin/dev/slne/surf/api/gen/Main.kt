package dev.slne.surf.api.gen

import dev.slne.surf.api.gen.data.Registries
import dev.slne.surf.api.gen.generator.Generators
import dev.slne.surf.api.gen.generator.SourceGenerator
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class Main

fun main() {
    val json = Json {
        ignoreUnknownKeys = true
    }

    val registryFile = Main::class.java.getResourceAsStream("/registries/registries.json")!!
    val registries = json.decodeFromString<Registries>(registryFile.readAllBytes().decodeToString())
    val generators = Generators(registries)

    generate(Path("../surf-api-core/surf-api-core-api/src/main/java"), generators.coreApiGenerators)
}

private fun generate(output: Path, generators: Array<SourceGenerator>) {
    output.createDirectories()

    for (generator in generators) {
        generator.writeToFile(output)
    }

    println("Generated ${generators.size} files to ${output.toAbsolutePath()}")
}