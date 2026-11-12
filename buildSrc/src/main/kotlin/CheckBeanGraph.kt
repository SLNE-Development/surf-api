import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

/**
 * Checks the bean metadata of a whole runtime classpath for faults that have no good moment later.
 *
 * KSP writes one metadata file per module, and the container merges them when it starts. A fault
 * that only exists in the merged set — the same bean class declared by two modules, two beans
 * claiming `@Primary` for one type, a dependency cycle running through three of them — is therefore
 * invisible to the per-module compile that produced it, and shows up when a server boots.
 *
 * The container does detect all of this at startup, and with better messages, because it knows
 * which conditions matched. This task is not a second authority: it is the same question asked
 * earlier, at a point where the answer costs a failed build rather than a failed server. So it
 * only asks what is decidable from the metadata alone and cannot become a false alarm:
 *
 * - **A duplicate class name.** Two modules declaring the same bean class means two definitions
 *   for one class, and the container would build it twice.
 * - **Two `@Primary` for one type.** The processor already catches this within a module; across
 *   modules nothing did.
 * - **A cycle.** Adding more modules cannot break one, so a cycle found in part of a classpath is
 *   a cycle in all of it.
 *
 * What it deliberately does not check is whether every required parameter has a provider. That is
 * only sound for a *complete* runtime: a module compiled on its own legitimately depends on a bean
 * a sibling contributes, and reporting it here would train everyone to ignore the task. The
 * container answers that one, where the whole set is known.
 *
 * Should the two ever disagree, the container is right. The drift is safe in that direction: this
 * task can only ever be weaker than the check that actually guards a start.
 */
@CacheableTask
abstract class CheckBeanGraph : DefaultTask() {

    /** The runtime classpath whose merged bean metadata is checked. */
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    /**
     * Written when the check passes, so Gradle can skip a run whose classpath has not changed.
     *
     * A task with no output is never up to date, and this one runs on every build.
     */
    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val beans = classpath.files.flatMap(::readBeans)
        val faults = duplicateClassNames(beans) + duplicatePrimaries(beans) + cycles(beans)

        if (faults.isNotEmpty()) {
            throw GradleException(
                "The merged bean metadata of ${path}'s runtime classpath is faulty:\n" +
                        faults.joinToString("\n") { "  - $it" }
            )
        }

        report.get().asFile.writeText("${beans.size} beans, no faults\n")
    }

    /** Two modules describing the same bean class. */
    private fun duplicateClassNames(beans: List<Bean>): List<String> = beans
        .groupBy { it.className }
        .filterValues { it.size > 1 }
        .map { (className, duplicates) ->
            "$className is declared by ${duplicates.size} modules (${
                duplicates.map { it.source }.distinct().sorted().joinToString()
            }). The container would build it once per definition."
        }

    /** Two beans claiming to be the one to pick for a type. */
    private fun duplicatePrimaries(beans: List<Bean>): List<String> = beans
        .filter { it.primary }
        .flatMap { bean -> bean.provides.map { type -> type to bean } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
        .map { (type, claimants) ->
            "$type is provided by ${claimants.size} beans marked @Primary (${
                claimants.map { it.className }.sorted().joinToString()
            }). Keep one, or give the others a @Qualifier."
        }

    /**
     * Cycles among the beans, as the chain of class names that closes.
     *
     * Walks the graph of "this bean needs a bean that provides this type", skipping the parameters
     * the container resolves later — a `Lazy` or a collection creates no edge, and an optional
     * parameter that nothing provides is simply absent.
     */
    private fun cycles(beans: List<Bean>): List<String> {
        val byType = beans.flatMap { bean -> bean.provides.map { it to bean } }
            .groupBy({ it.first }, { it.second })

        val edges = beans.associateWith { bean ->
            bean.needs.flatMap { byType[it].orEmpty() }.filter { it !== bean }.distinct()
        }

        val found = mutableListOf<String>()
        val finished = mutableSetOf<Bean>()
        val path = mutableListOf<Bean>()
        val onPath = mutableSetOf<Bean>()

        fun walk(bean: Bean) {
            if (bean in finished) return

            if (!onPath.add(bean)) {
                val cycle = path.dropWhile { it !== bean } + bean
                found += cycle.joinToString(" -> ") { it.className }
                return
            }

            path += bean
            edges.getValue(bean).forEach(::walk)
            path.removeLast()
            onPath -= bean
            finished += bean
        }

        beans.sortedBy { it.className }.forEach(::walk)

        // One cycle is reported once per entry point that reaches it; the chain is the same set.
        return found.distinctBy { it.split(" -> ").toSet() }
    }

    /** Every bean described under `META-INF/surf-api/beans` in [entry], jar or class directory. */
    private fun readBeans(entry: java.io.File): List<Bean> = when {
        entry.isDirectory -> entry.resolve(BEAN_DIRECTORY)
            .listFiles { file -> file.extension == "json" }
            .orEmpty()
            .flatMap { parse(it.readText(), "${entry.name}/${it.name}") }

        entry.extension == "jar" -> ZipFile(entry).use { jar ->
            jar.entries().asSequence()
                .filter { it.name.startsWith("$BEAN_DIRECTORY/") && it.name.endsWith(".json") }
                .flatMap { file ->
                    parse(jar.getInputStream(file).reader().readText(), "${entry.name}!${file.name}")
                }
                .toList()
        }

        else -> emptyList()
    }

    /**
     * The beans in one metadata file.
     *
     * Parsed with Groovy's slurper rather than the module's own serializer, because buildSrc cannot
     * depend on a module of the build it configures. Only the four fields this task judges are
     * read, so a schema that grows does not break the check — and a schema that *renames* one of
     * these four makes the task see no beans, which is why it reports the count it found.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parse(json: String, source: String): List<Bean> {
        val root = JsonSlurper().parseText(json) as Map<String, Any?>
        val beans = root["beans"] as? List<Map<String, Any?>> ?: return emptyList()

        return beans.map { bean ->
            Bean(
                className = bean["className"] as String,
                provides = (bean["provides"] as? List<String>).orEmpty(),
                primary = bean["primary"] as? Boolean == true,
                needs = (bean["ctorParams"] as? List<Map<String, Any?>>).orEmpty()
                    .filter { it["lazy"] != true && it["collection"] == null && it["optional"] != true }
                    .mapNotNull { it["type"] as? String },
                source = source,
            )
        }
    }

    /** What this task needs to know about a bean; the metadata holds a good deal more. */
    private data class Bean(
        val className: String,
        val provides: List<String>,
        val primary: Boolean,
        val needs: List<String>,
        val source: String,
    )

    private companion object {
        const val BEAN_DIRECTORY = "META-INF/surf-api/beans"
    }
}
