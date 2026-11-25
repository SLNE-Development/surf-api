package dev.slne.surf.api.gradle.internalapi

import dev.slne.surf.api.gradle.pluginclass.VerificationTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Fails the build when the project's compiled classes reference internal API of a dependency.
 *
 * Internal API is every declaration annotated with an internal API marker: an annotation whose
 * class carries `@InternalAPIMarker`, such as `@InternalSurfApi` or a plugin's own marker.
 * `@PublishedSurfApi` declarations are exempt, because inlined surf-api code references them.
 * The check reads bytecode, so an `@OptIn` in the source does not exempt a reference.
 */
@CacheableTask
abstract class CheckInternalApiUsage : VerificationTask() {

    /**
     * The compiled classes of the project.
     */
    @get:Classpath
    abstract val classes: ConfigurableFileCollection

    /**
     * The classpath the project compiles against, where referenced declarations are looked up.
     */
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    /**
     * Binary names of the markers whose declarations the project may reference.
     */
    @get:Input
    abstract val allowedMarkers: SetProperty<String>

    @TaskAction
    fun check() {
        val roots = classes.files.filter(File::isDirectory)
        val violations = ClassIndex(roots + classpath.files).use { index ->
            InternalApiUsageScanner(index, allowedMarkers.get()).scan(roots)
        }
        if (violations.isNotEmpty()) throw GradleException(failureMessage(violations))

        markPassed()
    }

    private fun failureMessage(violations: List<InternalApiViolation>): String {
        val markers = violations
            .flatMap { it.markers }
            .distinct()
            .sorted()
        return "The project references internal API of its dependencies, which may change or " +
            "disappear in any release:\n" +
            violations.joinToString("\n") { "  - $it" } +
            "\nUse public API instead. A surf library that is meant to use this API allows its " +
            "markers in the surf extension: " +
            "allowInternalApi(${markers.joinToString { "\"$it\"" }})"
    }
}
