import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Fails when a module imports a package it is not allowed to use.
 *
 * Some dependencies are a decision, not a detail: once a collection library appears in a published
 * signature, every consumer has to put it on their own compile classpath and every serialisation
 * boundary pays a copy to get back to a standard type. A review catches that once; a rule catches it
 * every time.
 *
 * The check reads imports rather than the compiled ABI, which makes it a slightly blunter
 * instrument — it also catches a package used purely internally — but it needs no compilation and
 * its failure points at a line a person can fix.
 */
abstract class CheckForbiddenImports : DefaultTask() {

    /** Path of the module being checked, used in the report. */
    @get:Input
    abstract val modulePath: Property<String>

    /** Package prefixes this module may not import, each with the reason it is forbidden. */
    @get:Input
    abstract val forbidden: ListProperty<ForbiddenPackage>

    /** The sources to read. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val rules = forbidden.get()
        if (rules.isEmpty()) return

        val findings = sources.files
            .asSequence()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file ->
                file.useLines { lines ->
                    lines.withIndex()
                        .filter { (_, line) -> line.trimStart().startsWith("import ") }
                        .flatMap { (number, line) ->
                            rules.filter { rule -> line.contains(rule.packagePrefix) }
                                .map { rule -> Finding(file.path, number + 1, line.trim(), rule) }
                        }
                        .toList()
                }
            }
            .toList()

        if (findings.isEmpty()) {
            logger.lifecycle("{}: no forbidden imports", modulePath.get())
            return
        }

        throw GradleException(
            buildString {
                appendLine("${modulePath.get()} imports packages it may not use:")
                findings.forEach { appendLine("  ${it.file}:${it.line}  ${it.statement}") }
                rules.filter { rule -> findings.any { it.rule == rule } }
                    .forEach { appendLine("  ${it.packagePrefix} — ${it.reason}") }
            }
        )
    }

    private data class Finding(
        val file: String,
        val line: Int,
        val statement: String,
        val rule: ForbiddenPackage,
    )
}

/**
 * A package a module may not import, and why.
 *
 * The reason travels with the rule so the failure explains itself instead of sending the reader
 * looking for the commit that added it.
 */
data class ForbiddenPackage(val packagePrefix: String, val reason: String) : java.io.Serializable
