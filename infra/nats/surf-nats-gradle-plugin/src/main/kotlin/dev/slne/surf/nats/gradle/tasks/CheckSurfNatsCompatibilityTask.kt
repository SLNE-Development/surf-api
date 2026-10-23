package dev.slne.surf.nats.gradle.tasks

import dev.slne.surf.nats.gradle.compat.CompatibilityChecker
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

/**
 * Fails when the `.proto` sources are incompatible with the baseline jars' `META-INF/surf-nats/proto` files.
 *
 * Skipped when [checkEnabled] is false or no baseline is configured. Warnings (shifted positional Kotlin parameters)
 * are logged and written to [reportFile] without failing the task.
 */
@CacheableTask
abstract class CheckSurfNatsCompatibilityTask : ProtoInputsTask() {

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoSources: FileTree
        get() = protoFiles(sourceDirectories)

    /**
     * Proto roots extracted by [ProtoArchiveTransform] from [baselineJars].
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineArchives: ConfigurableFileCollection

    /**
     * The resolved baseline artifacts; only named in the error for a baseline without protos.
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
        onlyIf("a compatibility baseline is configured (surfNats.compatibilityBaseline)", BaselineConfigured())
    }

    @TaskAction
    fun check() {
        val baselineDirs = baselineArchives.files.toList()
        if (baselineDirs.isEmpty()) {
            throw GradleException(
                "The surf-nats compatibility baseline (${baselineJars.files.joinToString { it.name }}) contains no " +
                        "${ProtoArchives.PREFIX}**/*.proto files"
            )
        }

        val result = try {
            CompatibilityChecker.check(baselineDirs, sourceDirectories.files.toList(), protoPath())
        } catch (e: SurfNatsGeneratorException) {
            throw GradleException(e.message.orEmpty(), e)
        }
        val violations = result.violations

        for (warning in result.warnings) logger.warn("surf-nats compatibility warning: $warning")
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()
        report.writeText(
            buildString {
                if (violations.isEmpty()) append("compatible\n") else violations.joinTo(this, "\n", postfix = "\n")
                result.warnings.joinTo(this, "") { "warning: $it\n" }
            },
            Charsets.UTF_8,
        )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "surf-nats contracts are incompatible with the baseline (${violations.size} violations):\n" +
                        violations.joinToString("\n") + "\nFix these changes or publish them in a new major package version (.v<N+1>)."
            )
        }
    }

    private class CheckEnabled : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean = (element as CheckSurfNatsCompatibilityTask).checkEnabled.get()
    }

    private class BaselineConfigured : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean =
            (element as CheckSurfNatsCompatibilityTask).baselineConfigured.get()
    }
}
