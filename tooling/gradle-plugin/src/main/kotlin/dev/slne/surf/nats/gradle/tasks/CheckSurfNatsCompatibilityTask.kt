
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
import org.gradle.api.tasks.*
import java.io.File

/**
 * Checks whether the current `.proto` sources are compatible with the previous release of the module.
 *
 * The plugin registers it as `checkSurfNatsCompatibility` and makes `check` and weaver's `publishProtos` depend on
 * it. The baseline is the release that [FetchSurfNatsBaselineTask] downloads into [baselineDirectory].
 *
 * Types and services are matched by their fully qualified proto names, so renaming or moving one counts as removing
 * it. Violations fail the task. Warnings are wire-compatible changes that still affect Kotlin callers. They are
 * logged and written to [reportFile] without failing the task.
 *
 * The task is skipped when [checkEnabled] or [active] is `false`, or when [baselineVersionFile] is missing or records
 * [FetchSurfNatsBaselineTask.NO_BASELINE]. To turn the check off:
 *
 * ```kotlin
 * surfNats {
 *     compatibilityCheck = false
 * }
 * ```
 */
@CacheableTask
abstract class CheckSurfNatsCompatibilityTask : ProtoInputsTask() {

    /**
     * The `.proto` files of [sourceDirectories], declared as task input.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoSources: FileTree
        get() = protoFiles(sourceDirectories)

    /**
     * The directories that hold the `.proto` files of the baseline release.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineDirectory: ConfigurableFileCollection

    /**
     * The file in which [FetchSurfNatsBaselineTask] records the baseline version or
     * [FetchSurfNatsBaselineTask.NO_BASELINE].
     */
    @get:Internal
    abstract val baselineVersionFile: RegularFileProperty

    /**
     * Whether the module is published or has an explicit `surfNats.compatibilityBaseline`.
     */
    @get:Internal
    abstract val active: Property<Boolean>

    /**
     * The value of `surfNats.compatibilityCheck`.
     */
    @get:Internal
    abstract val checkEnabled: Property<Boolean>

    /**
     * The report file, `build/reports/surf-nats/compatibility.txt` by default.
     *
     * It starts with `compatible` when there are no violations, and otherwise lists one violation per line. Each
     * warning follows on its own line, prefixed with `warning: `.
     */
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

    /**
     * Compares the sources with the baseline, logs the warnings and writes [reportFile].
     *
     * @throws GradleException when the baseline holds no `.proto` files, a schema cannot be loaded, or the check
     *   finds violations. The violation message lists each change and suggests a new major package version.
     */
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
