package dev.slne.surf.api.gen.nms

import java.nio.file.Path
import kotlin.io.path.*

/**
 * Generates version-specific NMS module source code from a reference module.
 *
 * The reference module (e.g. v26-1) serves as the canonical "template".
 * This generator copies every source file, applying version-specific
 * transformations:
 *
 * 1. Package name replacement (reference version → target version)
 * 2. Class prefix replacement (e.g. V26_1 → V1_21_11)
 * 3. Import replacements for changed NMS APIs
 * 4. Code-level replacements for renamed methods/classes
 * 5. Per-file custom transformers for structural differences
 */
class NmsTemplateGenerator(
    private val referenceVersionId: String,
    private val referenceClassPrefix: String,
    private val referenceSourceRoot: Path,
    private val targets: List<NmsVersionConfig>,
) {

    /**
     * Generate all target modules from the reference source files.
     *
     * @param repoRoot  absolute path to the repository root
     * @return number of files generated across all targets
     */
    fun generate(repoRoot: Path): Int {
        require(referenceSourceRoot.exists()) {
            "Reference source root does not exist: $referenceSourceRoot"
        }

        var totalGenerated = 0

        for (target in targets) {
            totalGenerated += generateTarget(repoRoot, target)
        }

        return totalGenerated
    }

    private fun generateTarget(repoRoot: Path, target: NmsVersionConfig): Int {
        val targetSourceRoot = repoRoot.resolve(target.sourceModulePath)
            .resolve("src/main/kotlin")

        // Clean existing generated sources
        if (targetSourceRoot.exists()) {
            targetSourceRoot.toFile().deleteRecursively()
        }
        targetSourceRoot.createDirectories()

        val sourceFiles = referenceSourceRoot.toFile()
            .walk()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        var generated = 0

        for (sourceFile in sourceFiles) {
            val relativePath = referenceSourceRoot.relativize(sourceFile.toPath())
            val transformedRelativePath = transformPath(relativePath, target)
            val targetFile = targetSourceRoot.resolve(transformedRelativePath)
            targetFile.parent.createDirectories()

            val originalContent = sourceFile.readText()
            val relativeKey = relativePath.toString().replace('\\', '/')
            val transformedContent = transformContent(originalContent, target, relativeKey)

            targetFile.writeText(transformedContent)
            generated++
        }

        println("Generated $generated files for ${target.versionId} in ${targetSourceRoot.toAbsolutePath()}")
        return generated
    }

    /**
     * Transforms a relative path from the reference module to the target module.
     * Replaces the reference package directory segments with target ones.
     */
    private fun transformPath(relativePath: Path, target: NmsVersionConfig): Path {
        val pathStr = relativePath.toString().replace('\\', '/')
        val transformed = pathStr
            .replace(referenceVersionId, target.versionId)
            .replace(referenceClassPrefix, target.classPrefix)
        return Path(transformed)
    }

    /**
     * Transforms file content from reference version to target version.
     */
    private fun transformContent(
        content: String,
        target: NmsVersionConfig,
        relativeKey: String,
    ): String {
        var result = content

        // 1. Replace package names (reference version → target version)
        result = result.replace(referenceVersionId, target.versionId)

        // 2. Replace class prefixes (e.g. V26_1 → V1_21_11)
        result = result.replace(referenceClassPrefix, target.classPrefix)

        // 3. Apply import-level replacements
        for ((old, new) in target.importReplacements) {
            result = result.replace(old, new)
        }

        // 4. Apply code-level replacements
        for ((old, new) in target.codeReplacements) {
            result = result.replace(old, new)
        }

        // 5. Apply per-file custom transformers
        val transformer = target.fileTransformers[relativeKey]
        if (transformer != null) {
            result = transformer(result)
        }

        return result
    }
}
