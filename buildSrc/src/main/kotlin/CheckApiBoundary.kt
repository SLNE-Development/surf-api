import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Reports dependencies that cross from an API module into an implementation module.
 *
 * The offending dependencies are collected while the project is configured and handed over as
 * plain strings, so the task action touches no project state and stays compatible with the
 * configuration cache.
 */
abstract class CheckApiBoundary : DefaultTask() {

    /** Path of the module being checked, used in the report. */
    @get:Input
    abstract val modulePath: Property<String>

    /** One entry per offending dependency, formatted as `configuration -> module`. */
    @get:Input
    abstract val violations: SetProperty<String>

    /** Whether a violation fails the build instead of only logging a warning. */
    @get:Input
    abstract val strict: Property<Boolean>

    @TaskAction
    fun check() {
        val found = violations.get().sorted()

        if (found.isEmpty()) {
            logger.lifecycle("{}: api boundary clean", modulePath.get())
            return
        }

        val report = buildString {
            appendLine("${modulePath.get()} is an API module but depends on implementation modules:")
            found.forEach { appendLine("  - $it") }
            append(
                "An API module may only depend on other API modules. Move the dependency to the " +
                        "matching implementation module, or move the type the API needs into an " +
                        "API module of its own."
            )
        }

        if (strict.get()) {
            throw GradleException(report)
        }

        logger.warn(report)
    }
}
