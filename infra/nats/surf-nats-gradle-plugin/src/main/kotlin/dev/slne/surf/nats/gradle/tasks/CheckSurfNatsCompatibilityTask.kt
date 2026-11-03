
package dev.slne.surf.nats.gradle.tasks

import dev.slne.surf.nats.gradle.compat.CompatibilityChecker
import dev.slne.surf.nats.gradle.compat.CompatibilityFinding
import dev.slne.surf.nats.gradle.generator.SurfNatsGeneratorException
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Checks whether the current proto sources are compatible with a baseline.
 *
 * The baseline is the previous release of this module's weaver artifact, fetched by
 * [FetchSurfNatsBaselineTask] into [baselineDirectory].
 *
 * The task is skipped when [checkEnabled] or [active] is false, or when [baselineVersionFile]
 * is missing or records [FetchSurfNatsBaselineTask.NO_BASELINE]. Compatibility violations fail the
 * task, while warnings are logged and written to [reportFile] without causing a failure.
 */
@CacheableTask
abstract class CheckSurfNatsCompatibilityTask : ProtoInputsTask() {

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoSources: FileTree
        get() = protoFiles(sourceDirectories)

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineDirectory: ConfigurableFileCollection

    /**
     * File written by [FetchSurfNatsBaselineTask] with the baseline version or [FetchSurfNatsBaselineTask.NO_BASELINE].
     */
    @get:Internal
    abstract val baselineVersionFile: RegularFileProperty

    /**
     * Whether the module is published or has an explicit `surfNats.compatibilityBaseline`.
     */
    @get:Internal
    abstract val active: Property<Boolean>

    @get:Internal
    abstract val checkEnabled: Property<Boolean>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    init {
        onlyIf("surfNats.compatibilityCheck is enabled", CheckEnabled())
        onlyIf(
            "the module is published or surfNats.compatibilityBaseline is set",
            Active(),
        )
        onlyIf("a released compatibility baseline exists in weaver", BaselineAvailable())
    }

    @TaskAction
    fun check() {
        val baselineDirectories = baselineDirectory.files.toList()
        validateBaseline(baselineDirectories)

        val result = try {
            CompatibilityChecker.check(
                baselineDirectories,
                sourceDirectories.files.toList(),
                protoPath(),
            )
        } catch (e: SurfNatsGeneratorException) {
            throw GradleException(e.message.orEmpty(), e)
        }

        for (warning in result.warnings) {
            logger.warn("surf-nats compatibility warning: $warning")
        }

        writeReport(result.violations, result.warnings)
        failOnViolations(result.violations)
    }

    /**
     * Ensures that the baseline contains proto files.
     */
    private fun validateBaseline(baselineDirectories: List<File>) {
        val hasProtos = baselineDirectories.any { directory ->
            directory.walkTopDown().any { it.isFile && it.extension == "proto" }
        }
        if (hasProtos) return

        throw GradleException(
            "The surf-nats compatibility baseline ${baselineVersion()} contains no .proto files"
        )
    }

    private fun baselineVersion(): String =
        FetchSurfNatsBaselineTask.readVersion(baselineVersionFile.get().asFile)

    /**
     * Writes compatibility violations and warnings to the report file.
     *
     * A successful check starts with `compatible`. Violations are written
     * one per line, followed by any warnings.
     */
    private fun writeReport(
        violations: List<CompatibilityFinding>,
        warnings: List<CompatibilityFinding>,
    ) {
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()

        val content = buildString {
            if (violations.isEmpty()) {
                append("compatible\n")
            } else {
                violations.joinTo(this, "\n", postfix = "\n")
            }

            warnings.joinTo(this, "") { "warning: $it\n" }
        }

        report.writeText(content, Charsets.UTF_8)
    }

    /**
     * Fails the task if compatibility violations were detected.
     */
    private fun failOnViolations(violations: List<CompatibilityFinding>) {
        if (violations.isEmpty()) return

        throw GradleException(
            "surf-nats contracts are incompatible with the baseline (${violations.size} violations):\n" +
                    violations.joinToString("\n") +
                    "\nFix these changes or publish them in a new major package version (.v<N+1>)."
        )
    }

    private class CheckEnabled : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean =
            (element as CheckSurfNatsCompatibilityTask).checkEnabled.get()
    }

    private class Active : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean =
            (element as CheckSurfNatsCompatibilityTask).active.get()
    }

    private class BaselineAvailable : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean =
            (element as CheckSurfNatsCompatibilityTask).baselineVersion() != FetchSurfNatsBaselineTask.NO_BASELINE
    }
}
