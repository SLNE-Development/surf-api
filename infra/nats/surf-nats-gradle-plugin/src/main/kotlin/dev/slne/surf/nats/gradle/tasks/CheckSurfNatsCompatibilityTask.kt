
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
 * Baseline proto files are read from `META-INF/surf-nats/proto` in the
 * configured baseline artifacts.
 *
 * The task is skipped when [checkEnabled] is false or no baseline is configured.
 * Compatibility violations fail the task, while warnings are logged and written
 * to [reportFile] without causing a failure.
 */
@CacheableTask
abstract class CheckSurfNatsCompatibilityTask : ProtoInputsTask() {

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoSources: FileTree
        get() = protoFiles(sourceDirectories)

    /**
     * Proto roots extracted from [baselineJars] by [ProtoArchiveTransform].
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineArchives: ConfigurableFileCollection

    /**
     * Resolved baseline artifacts, used to identify an empty baseline in errors.
     */
    @get:Internal
    abstract val baselineJars: ConfigurableFileCollection

    @get:Internal
    abstract val baselineConfigured: Property<Boolean>

    @get:Internal
    abstract val checkEnabled: Property<Boolean>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    init {
        onlyIf("surfNats.compatibilityCheck is enabled", CheckEnabled())
        onlyIf(
            "a compatibility baseline is configured (surfNats.compatibilityBaseline)",
            BaselineConfigured(),
        )
    }

    @TaskAction
    fun check() {
        val baselineDirectories = baselineArchives.files.toList()
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
     * Ensures that the baseline contains extracted proto roots.
     */
    private fun validateBaseline(baselineDirectories: List<File>) {
        if (baselineDirectories.isNotEmpty()) return

        val artifactNames = baselineJars.files.joinToString { it.name }

        throw GradleException(
            "The surf-nats compatibility baseline ($artifactNames) contains no " +
                    "${ProtoArchives.PREFIX}**/*.proto files"
        )
    }

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

    private class BaselineConfigured : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean =
            (element as CheckSurfNatsCompatibilityTask).baselineConfigured.get()
    }
}
