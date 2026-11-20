import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when a JVM ABI dump lists a nested class whose outer class it leaves out.
 *
 * The dump filters exclude a class marked with an internal annotation, but not the classes nested
 * in it: a companion object or a `Key` object of an internal class stays in the dump unless it
 * carries the marker itself. Such an entry freezes internal API as if it were public.
 */
abstract class CheckAbiDumpNesting : DefaultTask() {

    /**
     * The `.api` dump files to check; `.klib.api` dumps use another format and are not read.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dumps: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val orphans = dumps.files.flatMap { dump -> orphanedNestedClasses(dump.readText()).map { "${dump.name}: $it" } }
        if (orphans.isNotEmpty()) {
            throw GradleException(
                "ABI dumps list nested classes of excluded classes. Mark each with the module's internal " +
                    "annotation, as its outer class is:\n" + orphans.joinToString("\n") { "  - $it" },
            )
        }
    }
}

private val DUMPED_CLASS = Regex("""^public .*?class (\S+)""", RegexOption.MULTILINE)
private const val GENERATED_SERIALIZER = "\$\$serializer"

/**
 * The classes in [dump] that are nested in a class the dump does not list.
 */
fun orphanedNestedClasses(dump: String): List<String> {
    val classes = DUMPED_CLASS.findAll(dump).map { it.groupValues[1] }.toSet()
    return classes.filter { name -> outerClassOf(name)?.let { it !in classes } ?: false }.sorted()
}

private fun outerClassOf(name: String): String? = when {
    name.endsWith(GENERATED_SERIALIZER) -> name.removeSuffix(GENERATED_SERIALIZER)
    '$' in name.substringAfterLast('/') -> name.substringBeforeLast('$')
    else -> null
}
