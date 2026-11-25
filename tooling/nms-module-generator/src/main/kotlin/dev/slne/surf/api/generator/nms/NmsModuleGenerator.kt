package dev.slne.surf.api.generator.nms

import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Copies every `.kt` source file from a [reference] NMS module into a
 * [target] module, applying version-specific transformations along the way.
 *
 * A 26.x module keeps only its deltas to the shared sources in `platform/paper/nms/shared-26`.
 * Between two 26.x modules only those deltas are copied, and the target's build file applies the
 * shared sources. When only the reference uses them, the shared files the reference does not
 * override are copied too, so the target is complete.
 *
 * Processing order per file:
 * 1. Replace the version-id and class-prefix the file is written against, those of [reference] or
 *    of the version the shared sources are written as, with [target] equivalents.
 * 2. Apply each [NmsTransformation] from [transformations] in declaration order.
 *
 * Also writes `build.gradle.kts` when the target module has none.
 */
class NmsModuleGenerator(
    private val reference: NmsVersion,
    private val target: NmsVersion,
    private val transformations: List<NmsTransformation>,
    private val repoRoot: Path,
    private val logger: Logger,
) {

    private val referenceSourceRoot: Path = repoRoot.resolve(reference.modulePath).resolve(
        KOTLIN_SOURCE_PATH,
    )

    private val targetSourceRoot: Path = repoRoot.resolve(target.modulePath).resolve(
        KOTLIN_SOURCE_PATH,
    )

    private val sharedSourceRoot: Path = repoRoot.resolve(SHARED_26_MODULE_PATH).resolve(
        KOTLIN_SOURCE_PATH,
    )

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
        generateBuildFile()

        logger.lifecycle(
            "Generated {} source file(s) in {}",
            generated,
            targetSourceRoot.toAbsolutePath(),
        )

        return generated
    }

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

        val referenceSources = kotlinSources(
            referenceSourceRoot,
            reference.versionId,
            reference.classPrefix,
        )

        var count = 0

        for (source in referenceSources + sharedSourcesFor(referenceSources)) {
            val relativeKey = source.relativeKey

            if (exclusions.any { relativeKey.endsWith(it) || relativeKey == it }) {
                logger.info("Excluded: {}", relativeKey)
                continue
            }

            val targetPath = targetSourceRoot.resolve(source.targetPath())
            targetPath.parent.createDirectories()
            targetPath.writeText(transformContent(source.targetContent(), relativeKey))
            count++
        }

        return count
    }

    /**
     * The shared 26.x files a target outside the shared sources needs: those [referenceSources]
     * do not override. Empty when the target compiles the shared sources itself.
     */
    private fun sharedSourcesFor(referenceSources: List<SourceFile>): List<SourceFile> {
        if (!reference.usesShared26Template || target.usesShared26Template) return emptyList()

        val overridden = referenceSources.map(SourceFile::targetPath).toSet()
        val templateVersion = sharedTemplateVersion()

        return kotlinSources(sharedSourceRoot, templateVersion, templateVersion.uppercase())
            .filterNot { it.targetPath() in overridden }
    }

    private fun kotlinSources(
        root: Path,
        versionId: String,
        classPrefix: String,
    ): List<SourceFile> {
        if (!root.exists()) return emptyList()

        return root.toFile()
            .walk()
            .filter { it.isFile && it.extension == "kt" }
            .map { file ->
                val relativeKey = root.relativize(file.toPath()).toString().replace('\\', '/')
                SourceFile(file, relativeKey, versionId, classPrefix)
            }
            .toList()
    }

    /**
     * A source file and the version-id and class-prefix it is written against.
     */
    private inner class SourceFile(
        private val file: File,
        val relativeKey: String,
        private val versionId: String,
        private val classPrefix: String,
    ) {

        /**
         * The file's path below the target's source root.
         */
        fun targetPath(): String = retarget(relativeKey)

        /**
         * The file's content with its version identifiers replaced by the target's.
         */
        fun targetContent(): String = retarget(file.readText())

        private fun retarget(text: String): String {
            return text
                .replace(versionId, target.versionId)
                .replace(classPrefix, target.classPrefix)
        }
    }

    /**
     * Applies all transformations to a single file's content.
     */
    private fun transformContent(content: String, relativeKey: String): String {
        var result = content

        for (t in transformations) {
            result = apply(result, t, relativeKey)
        }

        return result
    }

    private fun apply(
        content: String,
        transformation: NmsTransformation,
        relativeKey: String,
    ): String {
        return when (transformation) {

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
                if (relativeKey.endsWith(transformation.filePattern) ||
                    relativeKey == transformation.filePattern
                ) {
                    transformation.transformer(content)
                } else {
                    content
                }

            is NmsTransformation.ExcludeFile ->
                content // already handled at file level
        }
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

    private fun buildGradleTemplate(): String {
        return """
        |plugins {
        |    id("surf.module-impl")
        |${sharedSourcesPluginLine()}    id("io.papermc.paperweight.userdev") apply true
        |}
        |
        |kotlin {
        |    compilerOptions {
        |        optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")
        |    }
        |}
        |
        |dependencies {
        |    api(projects.surfApiPaperNmsApi)
        |    api(projects.surfApiCoreImpl)
        |
        |    paperweight.paperDevBundle("${target.paperDevBundle}")
        |
        |    compileOnly(libs.fastutil)
        |
        |    compileOnly(libs.placeholder.api)
        |}
        |
        |configurations.all {
        |    exclude(group = "org.spigotmc", module = "spigot-api")
        |}
    """.trimMargin() + "\n"
    }

    /**
     * Reads the version the shared sources are written as from `shared-26/template.properties`, the
     * file the `surf.nms-shared-26` convention reads as well.
     */
    private fun sharedTemplateVersion(): String {
        val file = repoRoot.resolve(SHARED_26_MODULE_PATH).resolve(TEMPLATE_PROPERTIES).toFile()
        val properties = Properties()
        file.inputStream().use(properties::load)

        return checkNotNull(properties.getProperty("version")) { "$file declares no version" }
    }

    private fun sharedSourcesPluginLine(): String {
        return if (target.usesShared26Template) "    id(\"surf.nms-shared-26\")\n" else ""
    }

    private companion object {
        const val KOTLIN_SOURCE_PATH = "src/main/kotlin"
        const val SHARED_26_MODULE_PATH = "platform/paper/nms/shared-26"
        const val TEMPLATE_PROPERTIES = "template.properties"
    }
}
