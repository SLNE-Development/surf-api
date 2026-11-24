package dev.slne.surf.api.gradle.pluginclass

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipFile

/**
 * Finds Spring among the modules and artifacts a plugin jar bundles.
 *
 * surf-api ships Spring and plugins reach it through the class path, so a second copy inside a
 * plugin jar loads next to the first and splits the application context.
 */
internal object BundledSpring {
    private const val SPRING_GROUP = "org.springframework"
    private const val SPRING_PACKAGE_PATH = "org/springframework/"
    private const val MULTI_RELEASE_PATH = "META-INF/versions/"
    private val ARCHIVE_EXTENSIONS = setOf("jar", "zip")

    /**
     * Returns the entries of [modules] (`group:name:version`) that belong to Spring or a
     * Spring project (`org.springframework`, `org.springframework.boot`, ...).
     */
    fun find(modules: Collection<String>): List<String> {
        return modules.filter { module ->
            val group = module.substringBefore(':')
            group == SPRING_GROUP || group.startsWith("$SPRING_GROUP.")
        }.sorted()
    }

    /**
     * Returns the file names of the [artifacts] that contain unrelocated Spring classes or
     * resources, whatever coordinate they were resolved under.
     *
     * Only jar and zip archives are read. An entry counts when it lies under
     * `org/springframework/`, either at the root or in a multi-release directory; a relocated copy
     * does not.
     */
    fun findInArtifacts(artifacts: Iterable<File>): List<String> {
        return artifacts
            .filter { it.isFile && it.extension in ARCHIVE_EXTENSIONS && it.containsSpring() }
            .map { it.name }
            .sorted()
    }

    private fun File.containsSpring(): Boolean {
        return ZipFile(this).use { archive ->
            archive.stream().anyMatch { isSpringEntry(it.name) }
        }
    }

    private fun isSpringEntry(name: String): Boolean {
        val path = if (name.startsWith(MULTI_RELEASE_PATH)) {
            name.removePrefix(MULTI_RELEASE_PATH).substringAfter('/')
        } else {
            name
        }

        return path.startsWith(SPRING_PACKAGE_PATH)
    }
}

/**
 * Fails the build when the modules or artifacts shaded into the plugin jar include Spring.
 *
 * The modules and artifacts come from the configurations the shadow jar bundles. Includes and
 * excludes configured on the shadow jar itself are not considered; exclude Spring from the
 * configuration, or set `allowBundledSpring` when the jar is meant to carry it.
 */
@CacheableTask
abstract class CheckNoBundledSpringTask : VerificationTask() {

    /**
     * The modules that have an artifact in the configurations the shadow jar bundles, as
     * `group:name:version`. Modules without an artifact, such as a BOM, are not part of it.
     */
    @get:Input
    abstract val bundledModules: SetProperty<String>

    /**
     * The artifact files of the configurations the shadow jar bundles. Scanned for Spring
     * classes that arrive under a non-Spring coordinate, such as a fat jar.
     */
    @get:Classpath
    abstract val bundledArtifacts: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val springModules = BundledSpring.find(bundledModules.get())
        val springArtifacts = BundledSpring.findInArtifacts(bundledArtifacts)

        if (springModules.isNotEmpty() || springArtifacts.isNotEmpty()) {
            throw GradleException(
                "The plugin jar bundles Spring: ${describe(springModules, springArtifacts)}. " +
                    "surf-api provides Spring at runtime; declare it as compileOnly instead of shading it, " +
                    "or set allowBundledSpring = true in the surf extension.",
            )
        }

        markPassed()
    }

    private fun describe(springModules: List<String>, springArtifacts: List<String>): String {
        return buildList {
            if (springModules.isNotEmpty()) {
                add("modules ${springModules.joinToString()}")
            }

            if (springArtifacts.isNotEmpty()) {
                add(
                    "org/springframework classes in ${springArtifacts.joinToString()}",
                )
            }
        }.joinToString("; ")
    }
}
