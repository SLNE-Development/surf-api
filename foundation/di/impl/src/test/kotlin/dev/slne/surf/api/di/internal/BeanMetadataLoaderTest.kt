package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.definition.BeanMetadata
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Exercises the loader against a real classloader.
 *
 * Every case builds actual files and an actual jar in a temporary directory and points a
 * [URLClassLoader] at them, because the two URL kinds the loader has to handle — `file:` and
 * `jar:` — are produced by the classloader itself. A mocked classloader would let the test decide
 * what a URL looks like, which is exactly the part that has to be right.
 */
class BeanMetadataLoaderTest {

    @Test
    fun `an empty classpath yields empty metadata`() {
        assertTrue(loadFrom().beans.isEmpty())
    }

    @Test
    fun `two files in one directory merge`(@TempDir temp: Path) {
        val root = metadataDirectory(
            temp.resolve("classes"),
            "one.json" to beansJson("a.First"),
            "two.json" to beansJson("a.Second"),
        )

        assertEquals(listOf("a.First", "a.Second"), loadFrom(root).classNames())
    }

    @Test
    fun `a jar contributes its entries`(@TempDir temp: Path) {
        val jar = metadataJar(
            temp.resolve("module.jar"),
            "module.json" to beansJson("a.InJar", "a.AlsoInJar"),
        )

        assertEquals(listOf("a.InJar", "a.AlsoInJar"), loadFrom(jar).classNames())
    }

    @Test
    fun `a directory and a jar merge`(@TempDir temp: Path) {
        val root = metadataDirectory(temp.resolve("classes"), "local.json" to beansJson("a.Local"))
        val jar = metadataJar(temp.resolve("module.jar"), "module.json" to beansJson("a.Packaged"))

        assertEquals(listOf("a.Local", "a.Packaged"), loadFrom(root, jar).classNames())
    }

    @Test
    fun `a directory with no metadata files yields empty metadata`(@TempDir temp: Path) {
        val root = metadataDirectory(temp.resolve("classes"))

        assertTrue(loadFrom(root).beans.isEmpty())
    }

    @Test
    fun `a file that is not json is ignored`(@TempDir temp: Path) {
        val root = metadataDirectory(
            temp.resolve("classes"),
            "module.json" to beansJson("a.Only"),
            "README.txt" to "not metadata, and not json either",
        )

        assertEquals(listOf("a.Only"), loadFrom(root).classNames())
    }

    @Test
    fun `malformed json fails naming the file`(@TempDir temp: Path) {
        val root = metadataDirectory(
            temp.resolve("classes"),
            "broken.json" to """{"beans":[{"className":""",
        )
        val broken = root.resolve(BeanMetadata.DIRECTORY).resolve("broken.json")

        val failure = assertFailsWith<BeanMetadataException> { loadFrom(root) }

        assertContains(failure.message.orEmpty(), broken.toString())
    }

    @Test
    fun `malformed json in a jar fails naming the entry`(@TempDir temp: Path) {
        val jar = metadataJar(temp.resolve("module.jar"), "broken.json" to "[]")

        val failure = assertFailsWith<BeanMetadataException> { loadFrom(jar) }

        assertContains(failure.message.orEmpty(), jar.toString())
        assertContains(failure.message.orEmpty(), "broken.json")
    }

    @Test
    fun `the same bean class in two sources fails naming both`(@TempDir temp: Path) {
        val root = metadataDirectory(temp.resolve("classes"), "local.json" to beansJson("a.Twice"))
        val jar = metadataJar(temp.resolve("module.jar"), "module.json" to beansJson("a.Twice"))
        val local = root.resolve(BeanMetadata.DIRECTORY).resolve("local.json")

        val failure = assertFailsWith<BeanMetadataException> { loadFrom(root, jar) }
        val message = failure.message.orEmpty()

        assertContains(message, "a.Twice")
        assertContains(message, local.toString())
        assertContains(message, jar.toString())
    }

    @Test
    fun `a directory path containing a space is read`(@TempDir temp: Path) {
        val root = metadataDirectory(
            temp.resolve("out dir/main classes"),
            "module.json" to beansJson("a.Spaced"),
        )

        assertEquals(listOf("a.Spaced"), loadFrom(root).classNames())
    }

    @Test
    fun `a jar path containing a space is read`(@TempDir temp: Path) {
        val jar = metadataJar(
            temp.resolve("lib dir/my module.jar"),
            "module.json" to beansJson("a.SpacedJar"),
        )

        assertEquals(listOf("a.SpacedJar"), loadFrom(jar).classNames())
    }
}

/** The owner every case loads for; the loader only passes it on to the platform. */
private val owner = Any()

/** Loads metadata from a classloader covering exactly [roots], and nothing the test JVM carries. */
private fun loadFrom(vararg roots: Path): BeanMetadata {
    val urls = roots.map { it.toUri().toURL() }.toTypedArray()

    // No parent: the surrounding build puts its own metadata on the test classpath, and inheriting
    // it would make what a case sees depend on the rest of the repository.
    return URLClassLoader(urls, null).use { loader ->
        BeanMetadataLoader(FixedClassLoaderPlatform(loader)).load(owner)
    }
}

/** Writes [files] into [root]'s metadata directory and returns [root], ready as a classpath entry. */
private fun metadataDirectory(root: Path, vararg files: Pair<String, String>): Path {
    val directory = Files.createDirectories(root.resolve(BeanMetadata.DIRECTORY))

    files.forEach { (name, content) -> Files.writeString(directory.resolve(name), content) }

    return root
}

/** Packs [files] into a jar at [jar], directory entries included so the classloader finds them. */
private fun metadataJar(jar: Path, vararg files: Pair<String, String>): Path {
    Files.createDirectories(jar.parent)

    JarOutputStream(Files.newOutputStream(jar)).use { out ->
        val segments = BeanMetadata.DIRECTORY.split('/')

        segments.indices.forEach { depth ->
            out.putNextEntry(JarEntry(segments.take(depth + 1).joinToString("/", postfix = "/")))
            out.closeEntry()
        }

        files.forEach { (name, content) ->
            out.putNextEntry(JarEntry("${BeanMetadata.DIRECTORY}/$name"))
            out.write(content.toByteArray())
            out.closeEntry()
        }
    }

    return jar
}

/** The metadata text one module would contribute for [classNames]. */
private fun beansJson(vararg classNames: String) = classNames.joinToString(
    separator = ",",
    prefix = """{"beans":[""",
    postfix = "]}",
) { """{"className":"$it","provides":["$it"]}""" }

private fun BeanMetadata.classNames() = beans.map { it.className }

/** A platform that answers with one classloader and nothing else. */
private class FixedClassLoaderPlatform(private val classLoader: ClassLoader) : ContainerPlatform {

    override fun classLoader(owner: Any): ClassLoader = classLoader

    override fun isPluginLoaded(pluginId: String) = false

    override fun logger(owner: Any): PlatformLogger = SilentLogger

    override fun dataPath(owner: Any): Path = Paths.get("")

    override fun environments(): Set<String> = emptySet()
}

/** Swallows the loader's diagnostics; the cases assert on results and failures, not on log lines. */
private object SilentLogger : PlatformLogger {

    override fun info(message: String) = Unit

    override fun warn(message: String) = Unit

    override fun error(message: String, throwable: Throwable?) = Unit

    override fun debug(message: String) = Unit
}
