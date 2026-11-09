package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.definition.BeanMetadata
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Collects the bean metadata of every module visible to one owner.
 *
 * The metadata of a module travels in that module's own jar under [BeanMetadata.DIRECTORY], so the
 * whole classpath has to be enumerated rather than asked: every jar and every output directory on
 * the owner's classloader may contribute one file, and the container's beans are the merge of all
 * of them. Which classloader that is is the platform's answer, which is why the loader asks
 * [ContainerPlatform.classLoader] instead of using its own.
 *
 * Finding nothing is a legitimate result: an application that declares no beans gets
 * [BeanMetadata.empty] and an empty container, not a failure.
 *
 * ### Why unreadable metadata is fatal
 *
 * A file that cannot be parsed, a resource behind a protocol that cannot be walked and a `file:`
 * resource that is not a directory all fail with [BeanMetadataException] naming the source. The
 * tempting alternative — log the failure and carry on — turns one readable error into an
 * unreadable one: the module contributes no beans, and every bean that needed one of them then
 * fails with "nothing provides X" somewhere far away from the file that is actually broken. The
 * loader therefore refuses to produce a half-loaded container.
 *
 * ### Why a duplicate names both sources
 *
 * [BeanMetadata.plus] rejects a class that is defined twice, deliberately: a duplicate means a
 * module reached the classpath through two different jars, and picking one of them hides the cause
 * of whatever breaks later. That check only knows the class name, though, and "defined twice" with
 * nothing to look at is not actionable. The loader remembers which source every class came from,
 * so the failure it rethrows names both the file that is being read and the file that defined the
 * class first.
 *
 * ### Why URLs are never taken apart as strings
 *
 * A classpath entry may live under a path containing spaces or any other character a URL escapes,
 * and on Windows it carries a drive letter. Both survive only if the URL is converted through
 * [java.net.URI] — a `file:` resource through [Paths.get], a `jar:` resource through
 * [JarURLConnection], which hands over the jar and the entry name already separated. Cutting the
 * URL at `!` and feeding the remainder back to a file constructor mangles the first path with a
 * space in it.
 */
internal class BeanMetadataLoader(private val platform: ContainerPlatform) {

    /**
     * Reads and merges every metadata file [owner] owns.
     *
     * The locations come from [ContainerPlatform.beanSources] rather than from the owner's
     * classloader. A classloader's `getResources` also returns its parents' matches, which on a
     * platform where surf-api sits above an application would make that application's container
     * declare surf-api's beans as well as its own.
     *
     * Sources are visited in the order the platform gives them and the files within one source in
     * name order, so the merged list is the same on every run of the same classpath and a duplicate
     * always reports the same pair of sources.
     *
     * @throws BeanMetadataException if a source cannot be walked, a file cannot be parsed, or two
     * sources define the same bean class.
     */
    fun load(owner: Any): BeanMetadata {
        val logger = platform.logger(owner)
        val merge = Merge()

        for (url in platform.beanSources(owner)) {
            logger.debug("Reading bean metadata from $url")

            when (url.protocol) {
                "jar" -> readJar(url, merge)
                "file" -> readDirectory(url, merge)
                else -> throw BeanMetadataException(
                    "Bean metadata at $url cannot be read: the '${url.protocol}' protocol is not " +
                            "supported, only 'jar' and 'file' are. The beans of that module would " +
                            "be missing without anything saying so."
                )
            }
        }

        return merge.result.also {
            logger.debug("Loaded ${it.beans.size} bean definitions from ${merge.sources} metadata files")
        }
    }

    /**
     * Reads every `*.json` entry under the directory [url] points into, inside its jar.
     *
     * Nested entries count too: the processor writes one flat file per module, but accepting a
     * deeper layout costs nothing and silently dropping it would cost a whole module's beans.
     */
    private fun readJar(url: URL, merge: Merge) {
        val connection = url.openConnection() as JarURLConnection

        // The jar is this loader's to close, which only holds while the connection is uncached.
        connection.useCaches = false

        val directory = connection.entryName?.trimEnd('/').orEmpty()
        val prefix = if (directory.isEmpty()) "" else "$directory/"
        val jarName = describe(connection.jarFileURL)

        openJar(connection, url).use { jar ->
            jar.entries().asSequence()
                .filter { it.isMetadataFile(prefix) }
                .sortedBy { it.name }
                .forEach { entry ->
                    val source = "$jarName!/${entry.name}"
                    merge.add(parse(readEntry(jar, entry, source), source), source)
                }
        }
    }

    /** Reads every `*.json` file under the directory [url] points at. */
    private fun readDirectory(url: URL, merge: Merge) {
        val directory = Paths.get(url.toURI())

        if (!Files.isDirectory(directory)) {
            throw BeanMetadataException(
                "Bean metadata at $directory cannot be read: ${BeanMetadata.DIRECTORY} is not a " +
                        "directory. Something is packaging a file where the per-module metadata " +
                        "files belong."
            )
        }

        Files.walk(directory).use { paths ->
            paths.filter { it.isMetadataFile() }
                .sorted()
                .forEach { file ->
                    val source = file.toString()
                    merge.add(parse(readFile(file, source), source), source)
                }
        }
    }

    private fun openJar(connection: JarURLConnection, url: URL): JarFile = try {
        connection.jarFile
    } catch (e: IOException) {
        throw BeanMetadataException("The jar holding bean metadata at $url cannot be opened", e)
    }

    private fun readEntry(jar: JarFile, entry: JarEntry, source: String): String = try {
        jar.getInputStream(entry).use { it.reader(Charsets.UTF_8).readText() }
    } catch (e: IOException) {
        throw BeanMetadataException("Bean metadata in $source cannot be read", e)
    }

    private fun readFile(file: Path, source: String): String = try {
        Files.readString(file)
    } catch (e: IOException) {
        throw BeanMetadataException("Bean metadata in $source cannot be read", e)
    }

    /**
     * Turns one file's text into metadata.
     *
     * The parse failure is wrapped rather than passed on because the message kotlinx.serialization
     * produces describes the offending offset and nothing about which of the classpath's files it
     * was found in.
     */
    private fun parse(text: String, source: String): BeanMetadata = try {
        BeanMetadata.json.decodeFromString(BeanMetadata.serializer(), text)
    } catch (e: SerializationException) {
        throw BeanMetadataException("Bean metadata in $source is not readable: ${e.message}", e)
    }

    /** A readable form of a jar's location, so a reported path is the one a person can look at. */
    private fun describe(jarFileUrl: URL): String =
        if (jarFileUrl.protocol == "file") Paths.get(jarFileUrl.toURI()).toString() else jarFileUrl.toString()

    private fun JarEntry.isMetadataFile(prefix: String) =
        !isDirectory && name.startsWith(prefix) && name.endsWith(JSON_SUFFIX)

    private fun Path.isMetadataFile() =
        Files.isRegularFile(this) && fileName.toString().endsWith(JSON_SUFFIX)

    /**
     * The merge in progress, plus where each bean came from.
     *
     * The origins exist only for the duplicate message: the merge itself stays
     * [BeanMetadata.plus], so what counts as a duplicate is decided in one place.
     */
    private class Merge {

        /** Everything merged so far. */
        var result: BeanMetadata = BeanMetadata.empty()
            private set

        /** How many files contributed, for the diagnostic line. */
        var sources: Int = 0
            private set

        private val sourceByClass = HashMap<String, String>()

        fun add(metadata: BeanMetadata, source: String) {
            result = try {
                result + metadata
            } catch (e: IllegalStateException) {
                throw duplicate(metadata, source, e)
            }

            metadata.beans.forEach { sourceByClass[it.className] = source }
            sources++
        }

        private fun duplicate(
            metadata: BeanMetadata,
            source: String,
            cause: IllegalStateException,
        ): BeanMetadataException {
            val collisions = metadata.beans
                .map { it.className }
                .filter { it in sourceByClass }
                .sorted()
                .joinToString { "$it (already defined in ${sourceByClass.getValue(it)})" }

            return BeanMetadataException(
                "Bean metadata in $source defines beans that another source already defined: " +
                        collisions.ifEmpty { cause.message.orEmpty() } +
                        ". A module most likely reached the classpath through two different jars.",
                cause,
            )
        }
    }

    private companion object {

        const val JSON_SUFFIX = ".json"
    }
}

/**
 * Raised when the bean metadata on the classpath cannot be read or contradicts itself.
 *
 * An [IllegalStateException] because that is what a broken classpath is, and because it keeps the
 * duplicate failure the same kind of exception [BeanMetadata.plus] raises.
 */
internal class BeanMetadataException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
