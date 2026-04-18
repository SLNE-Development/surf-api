package dev.slne.surf.api.generator.nms

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Generates a single NMS module from the configured reference version.
 *
 * If the target module already contains sources the task asks for
 * confirmation before overwriting. Pass `-PforceOverwrite` to skip
 * the prompt (useful in scripts or CI).
 */
abstract class GenerateNmsModuleTask : DefaultTask() {

    init {
        group = "generation"
        description = "Generates an NMS module from the reference version sources"
    }

    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(NmsGeneratorExtension::class.java)

        check(ext.isConfigured) {
            "nmsGenerator.referenceVersion and nmsGenerator.targetVersion must be set"
        }

        val reference = ext.referenceVersion
        val target = ext.targetVersion
        val repoRoot = project.rootProject.projectDir.toPath()

        confirmOverwriteIfNeeded(repoRoot, target)

        logger.lifecycle("Generating NMS module: {} -> {} ...", reference.name, target.name)

        val generated = NmsModuleGenerator(
            reference = reference,
            target = target,
            transformations = ext.transformationScope.transformations,
            repoRoot = repoRoot,
            logger = logger,
        ).generate()

        logger.lifecycle("")
        logger.lifecycle("------------------------------------------")
        logger.lifecycle("Generation complete - {} file(s) written.", generated)
        logger.lifecycle("")
        logger.lifecycle("Next steps:")
        logger.lifecycle("  1. Add include(\"{}\") to settings.gradle.kts (if not present).", target.gradlePath)
        logger.lifecycle("  2. Register {} in the runtime NmsVersion enum.", target.name)
        logger.lifecycle("  3. Review the generated sources and fix remaining compilation errors.")
        logger.lifecycle("------------------------------------------")
    }

    // ------------------------------------------------------------------ //

    private fun confirmOverwriteIfNeeded(repoRoot: Path, target: NmsVersion) {
        val targetSources = repoRoot
            .resolve(target.modulePath)
            .resolve("src/main/kotlin")

        if (!targetSources.exists()) return

        // Allow skipping the prompt via project property
        if (project.hasProperty("forceOverwrite")) return

        val console = System.console() ?: throw GradleException(
            "Target module ${target.name} already exists. " +
                    "Pass -PforceOverwrite to overwrite."
        )

        console.printf(
            "%nTarget module '%s' already has sources at:%n  %s%n",
            target.name,
            targetSources.toAbsolutePath(),
        )
        val answer = console.readLine("Overwrite? [y/N] ")?.trim()?.lowercase()

        if (answer != "y" && answer != "yes") {
            throw GradleException("Aborted by user.")
        }
    }
}

