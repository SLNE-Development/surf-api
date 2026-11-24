package dev.slne.surf.api.gradle.pluginclass

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

/**
 * Looks up the superclass of a class by its internal name (`a/b/C`).
 */
internal fun interface SuperclassLookup {
    /**
     * Returns the internal name of the superclass of [internalName], or `null` for
     * `java/lang/Object` and for a class the lookup cannot find.
     */
    fun superclassOf(internalName: String): String?
}

/**
 * The outcome of following the superclass chain of a plugin class.
 */
internal sealed interface SuperclassChain {
    val chain: List<String>

    data class Reaches(override val chain: List<String>) : SuperclassChain
    data class Misses(override val chain: List<String>) : SuperclassChain
}

/**
 * Follows superclass links to decide whether a class extends a surf base class.
 */
internal object SuperclassChecker {
    /**
     * Follows the superclasses of [className] (a binary name such as `a.b.C`) until [expectedBase]
     * or the end of the chain.
     */
    fun check(className: String, expectedBase: String, lookup: SuperclassLookup): SuperclassChain {
        val chain = mutableListOf(className)
        var current = className.replace('.', '/')
        val target = expectedBase.replace('.', '/')
        val seen = mutableSetOf<String>()

        while (current != target) {
            if (!seen.add(current)) break
            val next = lookup.superclassOf(current) ?: break

            chain += next.replace('/', '.')
            current = next
        }

        return if (current == target) SuperclassChain.Reaches(chain) else SuperclassChain.Misses(
            chain,
        )
    }
}

/**
 * Reads class headers from class directories and jars, in that order.
 *
 * The lookup searches [directories] first, then the entries of [classpath]: jars and class
 * directories alike, because a project dependency contributes its class directory.
 *
 * Only the header is parsed, so the check works on classes compiled for a newer class file
 * version than the surrounding code and never loads a class.
 */
internal class ClassPathLookup(directories: Collection<File>, classpath: Collection<File>) :
    SuperclassLookup, Closeable {
    private val directories = (directories + classpath).filter { it.isDirectory }
    private val openJars = classpath.filter { it.isFile && it.extension == "jar" }.mapNotNull {
        try {
            JarFile(it)
        } catch (_: IOException) {
            null
        }
    }

    fun contains(className: String): Boolean = readHeader(className.replace('.', '/')) != null

    override fun superclassOf(internalName: String): String? = readHeader(internalName)?.superName

    private fun readHeader(internalName: String): ClassReader? {
        val entry = "$internalName.class"

        directories.firstNotNullOfOrNull { directory ->
            directory.resolve(entry).takeIf { it.isFile }?.inputStream()?.use { ClassReader(it) }
        }?.let { return it }

        return openJars.firstNotNullOfOrNull { jar ->
            jar.getJarEntry(entry)?.let {
                jar.getInputStream(it).use { stream -> ClassReader(stream) }
            }
        }
    }

    override fun close() {
        for (openJar in openJars) {
            openJar.close()
        }
    }
}

/**
 * Fails the build when a plugin class named in the build script does not extend the surf base
 * class of its platform.
 *
 * The compiled classes of the main source set come first, then the compile classpath, so the
 * chain may leave the plugin through a library class or the class directory of another project.
 */
@CacheableTask
abstract class CheckSurfPluginClassesTask : VerificationTask() {

    /**
     * The classes to check, mapped to the surf base class each must extend. Both are binary names.
     */
    @get:Input
    abstract val expectedBases: MapProperty<String, String>

    @get:Classpath
    abstract val classDirectories: ConfigurableFileCollection

    @get:Classpath
    abstract val compileClasspath: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val failures = ClassPathLookup(
            classDirectories.files,
            compileClasspath.files,
        ).use { lookup ->
            expectedBases.get().mapNotNull { (className, base) ->
                failureOf(
                    className,
                    base,
                    lookup,
                )
            }
        }

        if (failures.isNotEmpty()) {
            throw GradleException(failures.joinToString("\n"))
        }

        markPassed()
    }

    private fun failureOf(className: String, base: String, lookup: ClassPathLookup): String? {
        val declaredIn = GeneratedPluginClass.entries.firstOrNull { it.superclass == base }?.namedIn
            ?: "the build script"

        if (!lookup.contains(className)) {
            return "The class '$className' named in $declaredIn was not found among the compiled classes."
        }

        val result = SuperclassChecker.check(className, base, lookup)
        if (result is SuperclassChain.Reaches) return null

        return "The class '$className' named in $declaredIn must extend '$base', " +
            "but its superclass chain is ${result.chain.joinToString(" -> ")}. " +
            "Extend '$base' or remove the class from $declaredIn to use the generated one. " +
            "Disable this check with verifyPluginClasses = false."
    }
}
