package dev.slne.surf.microservice.gradle.mainclass

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Resolves the main class of a microservice and writes its binary name to [outputFile].
 *
 * [mainClass] wins when it is set. Otherwise the task scans [classDirectories] and requires exactly one class with
 * a `public static void main(String[])`. It fails for none and for several, so a wrong or ambiguous entry point
 * never reaches the jar manifest.
 */
@CacheableTask
abstract class DetectMicroserviceMainClassTask : DefaultTask() {

    /**
     * The compiled classes of the project's main source set.
     */
    @get:Classpath
    abstract val classDirectories: ConfigurableFileCollection

    /**
     * The main class set in `surfMicroservice { mainClass }` or, failing that, in `application { mainClass }`; absent to
     * detect it.
     */
    @get:Input
    @get:Optional
    abstract val mainClass: Property<String>

    /**
     * The path of the project, named in the failure messages.
     */
    @get:Input
    abstract val projectPath: Property<String>

    /**
     * Receives the binary name of the main class.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun resolve() {
        val resolved = mainClass.orNull?.takeIf { it.isNotBlank() } ?: detect()
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(resolved)
        }
    }

    private fun detect(): String {
        val candidates = findMainClasses(classDirectories.files)
        return when (candidates.size) {
            1 -> candidates.single()
            0 -> throw GradleException(
                "No main class found in the compiled classes of '${projectPath.get()}'. Declare a top-level " +
                        "'fun main(args: Array<String>) { runMicroservice<App>(args) }' next to the " +
                        "@SpringBootApplication class, or set surfMicroservice { mainClass = \"...\" }."
            )

            else -> throw GradleException(
                "Found ${candidates.size} main classes in '${projectPath.get()}': ${candidates.joinToString()}. " +
                        "Keep one, or choose with surfMicroservice { mainClass = \"...\" }."
            )
        }
    }
}
