package dev.slne.surf.api.generator.nms

import org.gradle.api.logging.Logger
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Copies every `.kt` source file from a [reference] NMS module into a
 * [target] module, applying version-specific transformations along the way.
 *
 * Processing order per file:
 * 1. Replace [reference] version-id and class-prefix with [target] equivalents.
 * 2. Apply each [NmsTransformation] from [transformations] in declaration order.
 *
 * Additionally generates:
 * - `META-INF/services/dev.slne.surf.api.paper.nms.common.NmsProvider` for ServiceLoader
 * - `build.gradle.kts` (only when absent)
 */
class NmsModuleGenerator(
    private val reference: NmsVersion,
    private val target: NmsVersion,
    private val transformations: List<NmsTransformation>,
    private val repoRoot: Path,
    private val logger: Logger,
) {

    private val referenceSourceRoot: Path =
        repoRoot.resolve(reference.modulePath).resolve(KOTLIN_SOURCE_PATH)

    private val targetSourceRoot: Path =
        repoRoot.resolve(target.modulePath).resolve(KOTLIN_SOURCE_PATH)

    // ------------------------------------------------------------------ //
    //  Public API                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Runs the full generation pipeline and returns the number of
     * generated source files.
     */
    fun generate(): Int {
        require(referenceSourceRoot.exists()) {
            "Reference source root does not exist: $referenceSourceRoot"
        }

        prepareTargetDirectory()
        val generated = copyAndTransformSources()
        generateServiceFile()
        generateBuildFile()

        logger.lifecycle(
            "Generated {} source file(s) in {}",
            generated,
            targetSourceRoot.toAbsolutePath(),
        )
        return generated
    }

    // ------------------------------------------------------------------ //
    //  Internal steps                                                     //
    // ------------------------------------------------------------------ //

    private fun prepareTargetDirectory() {
        if (targetSourceRoot.exists()) {
            targetSourceRoot.toFile().deleteRecursively()
        }
        targetSourceRoot.createDirectories()
    }

    private fun copyAndTransformSources(): Int {
        val exclusions = transformations
            .filterIsInstance<NmsTransformation.ExcludeFile>()
            .map(NmsTransformation.ExcludeFile::filePattern)

        val sourceFiles = referenceSourceRoot.toFile()
            .walk()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        var count = 0
        for (file in sourceFiles) {
            val relativeKey = referenceSourceRoot
                .relativize(file.toPath())
                .toString()
                .replace('\\', '/')

            if (exclusions.any { relativeKey.endsWith(it) || relativeKey == it }) {
                logger.info("Excluded: {}", relativeKey)
                continue
            }

            val targetPath = targetSourceRoot.resolve(transformPath(relativeKey))
            targetPath.parent.createDirectories()
            targetPath.writeText(transformContent(file.readText(), relativeKey))
            count++
        }
        return count
    }

    // ------------------------------------------------------------------ //
    //  Transformations                                                    //
    // ------------------------------------------------------------------ //

    /** Rewrites directory segments and file names for the target version. */
    private fun transformPath(relativeKey: String): String = relativeKey
        .replace(reference.versionId, target.versionId)
        .replace(reference.classPrefix, target.classPrefix)

    /** Applies all transformations to a single file's content. */
    private fun transformContent(content: String, relativeKey: String): String {
        // Always replace version-id and class-prefix first
        var result = content
            .replace(reference.versionId, target.versionId)
            .replace(reference.classPrefix, target.classPrefix)

        for (t in transformations) {
            result = apply(result, t, relativeKey)
        }
        return result
    }

    private fun apply(
        content: String,
        transformation: NmsTransformation,
        relativeKey: String,
    ): String = when (transformation) {

        is NmsTransformation.RenameClass -> {
            var result = content.replace(
                "import ${transformation.oldFqn}",
                "import ${transformation.newFqn}",
            )
            if (transformation.oldSimpleName != transformation.newSimpleName) {
                result = result.replace(
                    Regex("\\b${Regex.escape(transformation.oldSimpleName)}\\b"),
                    transformation.newSimpleName,
                )
            }
            result
        }

        is NmsTransformation.RemoveImport ->
            content
                .replace("import ${transformation.fqn}\n", "")
                .replace("import ${transformation.fqn}\r\n", "")

        is NmsTransformation.ReplaceCode ->
            content.replace(transformation.old, transformation.new)

        is NmsTransformation.ReplacePattern ->
            content.replace(transformation.pattern, transformation.replacement)

        is NmsTransformation.TransformFile ->
            if (relativeKey.endsWith(transformation.filePattern) || relativeKey == transformation.filePattern) {
                transformation.transformer(content)
            } else {
                content
            }

        is NmsTransformation.ExcludeFile ->
            content // already handled at file level
    }

    // ------------------------------------------------------------------ //
    //  Scaffold generation                                                //
    // ------------------------------------------------------------------ //

    private fun generateServiceFile() {
        val serviceDir = repoRoot
            .resolve(target.modulePath)
            .resolve("src/main/resources/META-INF/services")
        serviceDir.createDirectories()

        val providerFqn = "$PROVIDER_PACKAGE_PREFIX${target.versionId}.${target.classPrefix}NmsProvider"
        serviceDir.resolve(NMS_PROVIDER_SERVICE).writeText("$providerFqn\n")

        logger.lifecycle("Generated service file: {}", providerFqn)
    }

    private fun generateBuildFile() {
        val buildFile = repoRoot.resolve(target.modulePath).resolve("build.gradle.kts")
        if (buildFile.exists()) {
            logger.lifecycle("build.gradle.kts already exists, skipping")
            return
        }

        buildFile.parent.createDirectories()
        buildFile.writeText(buildGradleTemplate())
        logger.lifecycle("Generated build.gradle.kts (paperDevBundle={})", target.paperDevBundle)
    }

    private fun buildGradleTemplate(): String = """
        |plugins {
        |    `core-convention`
        |    id("io.papermc.paperweight.userdev") apply true
        |}
        |
        |kotlin {
        |    compilerOptions {
        |        optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")
        |    }
        |}
        |
        |dependencies {
        |    api(projects.surfApiPaper.surfApiPaperNms.surfApiPaperNmsCommon)
        |    api(projects.surfApiCore.surfApiCoreServer)
        |
        |    paperweight.paperDevBundle("${target.paperDevBundle}")
        |
        |    compileOnly(libs.placeholder.api)
        |    compileOnly(libs.reflection.remapper)
        |    compileOnly(libs.mccoroutine.folia.api)
        |    compileOnly(libs.scoreboard.library.api)
        |
        |    implementation(libs.bytebuddy)
        |}
        |
        |configurations.all {
        |    exclude(group = "org.spigotmc", module = "spigot-api")
        |}
    """.trimMargin() + "\n"

    private companion object {
        const val KOTLIN_SOURCE_PATH = "src/main/kotlin"
        const val PROVIDER_PACKAGE_PREFIX = "dev.slne.surf.api.paper.server.nms."
        const val NMS_PROVIDER_SERVICE = "dev.slne.surf.api.paper.nms.common.NmsProvider"
    }
}




