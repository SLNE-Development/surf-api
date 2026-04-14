package dev.slne.surf.api.gen.nms

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Entry point for NMS version-specific code generation.
 *
 * Usage: Run with repo root as the first argument.
 * From Gradle: `./gradlew :surf-api-generator:generateNms`
 */
fun main(args: Array<String>) {
    val repoRoot = if (args.isNotEmpty()) Path(args[0]) else Path("..")

    println("Generating NMS version-specific code from reference ${NmsVersionConfigs.REFERENCE_VERSION_ID}...")

    val referenceSourceRoot = repoRoot
        .resolve(NmsVersionConfigs.REFERENCE_MODULE_PATH)
        .resolve("src/main/kotlin")

    val generator = NmsTemplateGenerator(
        referenceVersionId = NmsVersionConfigs.REFERENCE_VERSION_ID,
        referenceClassPrefix = NmsVersionConfigs.REFERENCE_CLASS_PREFIX,
        referenceSourceRoot = referenceSourceRoot,
        targets = NmsVersionConfigs.ALL_TARGETS,
    )

    val totalGenerated = generator.generate(repoRoot)
    println("NMS generation complete: $totalGenerated files generated across ${NmsVersionConfigs.ALL_TARGETS.size} target(s)")
}
