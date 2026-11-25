package dev.slne.surf.api.generator.registry

import kotlin.io.path.Path

/**
 * Regenerates the registry key objects of `surf-api-registry-keys`.
 *
 * Existing files in the source root are overwritten.
 *
 * @param args the Minecraft version, the work directory that receives the server jar and the
 * vanilla data generator output, and the Kotlin source root the key objects are written to
 */
fun main(args: Array<String>) {
    require(args.size == 3) {
        "Usage: <minecraft version> <work directory> <kotlin source root>"
    }
    val (minecraftVersion, workDirectory, sourceRoot) = args
    val outputDirectory = Path(sourceRoot)

    val vanillaData = VanillaDataGenerator(Path(workDirectory)).generate(minecraftVersion)
    val files = RegistryKeySources.create(vanillaData)

    for (file in files) {
        file.writeTo(outputDirectory)
    }

    println("Generated ${files.size} files into ${outputDirectory.toAbsolutePath()}")
}
