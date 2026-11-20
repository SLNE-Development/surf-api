import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipFile

/**
 * Fails when the `META-INF/services` file of [service] in [jar] lacks one of [expectedProviders].
 *
 * A shadow jar that bundles several modules keeps only the first copy of a service file unless the copies reach the
 * service file transformer. The lost providers are not reported at build time and surface as missing beans at
 * startup, so the check reads the merged file instead.
 */
abstract class CheckShadowServiceFile : DefaultTask() {

    /**
     * The shadow jar to read.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jar: RegularFileProperty

    /**
     * Binary name of the service interface; names the file under `META-INF/services`.
     */
    @get:Input
    abstract val service: Property<String>

    /**
     * Binary names of the providers the file must list.
     */
    @get:Input
    abstract val expectedProviders: ListProperty<String>

    @TaskAction
    fun check() {
        val missing = missingServiceProviders(jar.get().asFile, service.get(), expectedProviders.get())
        if (missing.isNotEmpty()) {
            throw GradleException(
                "${jar.get().asFile.name} lists no provider ${missing.joinToString()} in META-INF/services/${service.get()}."
            )
        }
    }
}

/**
 * Returns the [expected] providers that the service file of [service] in [jar] does not list; all of them when the
 * jar has no such file.
 */
internal fun missingServiceProviders(jar: File, service: String, expected: List<String>): List<String> {
    val listed = ZipFile(jar).use { zip ->
        val entry = zip.getEntry("META-INF/services/$service") ?: return expected
        zip.getInputStream(entry).bufferedReader().readLines()
    }.map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.toSet()

    return expected.filterNot { it in listed }
}
