package dev.slne.surf.nats.gradle.schema

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaException
import com.squareup.wire.schema.SchemaLoader
import dev.slne.surf.nats.gradle.generator.SurfNatsGeneratorException
import okio.FileMetadata
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Source
import okio.source
import java.io.File

/**
 * A linked wire schema and the files of its source roots, sorted by relative path.
 */
class LoadedSchema(val schema: Schema, val sourceFiles: List<ProtoFile>)

/**
 * Loads [sourceDirs] against the bundled protos and [protoPathDirs]; proto-path copies of bundled paths are hidden.
 */
fun loadSchema(sourceDirs: List<File>, protoPathDirs: List<File>): LoadedSchema {
    val sources = sourceDirs.filter(File::isDirectory).map { it.absoluteFile.normalize() }.distinct()
    val protoPath = protoPathDirs.filter(File::isDirectory).map { it.absoluteFile.normalize() }.distinct()
    if (sources.none { it.containsProtos() }) {
        throw SurfNatsGeneratorException(listOf("${sourceDirs.joinToString()}: no .proto files found"))
    }

    val bundledInSources = sources.flatMap { dir ->
        BundledProtos.paths.filter { dir.resolve(it).isFile }.map { path ->
            "${Location.get(dir.path, path)}: $path is bundled with the surf-nats plugin and must not be a proto source"
        }
    }
    if (bundledInSources.isNotEmpty()) throw SurfNatsGeneratorException(bundledInSources)

    val hidden = protoPath.flatMapTo(HashSet()) { dir -> BundledProtos.paths.map { canonical(dir.resolve(it)) } }
    val loader = SchemaLoader(ProtoFileSystem(hidden))
    loader.initRoots(
        sourcePath = sources.map { Location.get(it.path) },
        protoPath = listOf(Location.get(BundledProtos.ROOT)) + protoPath.map { Location.get(it.path) },
    )

    val schema = try {
        loader.loadSchema()
    } catch (e: SchemaException) {
        throw SurfNatsGeneratorException(WireErrors.translate(e.message.orEmpty(), sources))
    } catch (e: IllegalStateException) {
        throw SurfNatsGeneratorException(listOf(WireErrors.translateSyntaxError(e.message.orEmpty())))
    }

    val sourceLocations = loader.sourcePathFiles.mapTo(HashSet()) { it.location.base to it.location.path }
    val sourceFiles = schema.protoFiles
        .filter { (it.location.base to it.location.path) in sourceLocations }
        .sortedWith(compareBy({ it.location.path }, { it.location.base }))
    return LoadedSchema(schema, sourceFiles)
}

private fun canonical(file: File): File = file.absoluteFile.normalize()

/**
 * Serves the bundled protos below the relative root [BundledProtos.ROOT] from the plugin classpath and hides
 * proto-path files that shadow a bundled proto; wire probes proto-path roots with `exists`.
 *
 * Source and proto-path roots are absolute, so the relative root cannot clash with them.
 */
private class ProtoFileSystem(private val hidden: Set<File>) : ForwardingFileSystem(FileSystem.SYSTEM) {
    override fun metadataOrNull(path: Path): FileMetadata? {
        val bundled = bundledPath(path)
        if (bundled != null) {
            return when {
                bundled.isEmpty() -> DIRECTORY
                bundled in BundledProtos.paths -> REGULAR_FILE
                else -> null
            }
        }
        if (hidden.isNotEmpty() && canonical(path.toFile()) in hidden) return null
        return super.metadataOrNull(path)
    }

    override fun source(file: Path): Source {
        val bundled = bundledPath(file) ?: return super.source(file)
        return BundledProtos.open(bundled).source()
    }

    /**
     * Returns the path of [path] relative to [BundledProtos.ROOT] (empty for the root), or `null` outside of it.
     */
    private fun bundledPath(path: Path): String? {
        if (path.isAbsolute) return null
        val segments = path.segments
        if (segments.firstOrNull() != BundledProtos.ROOT) return null
        return segments.drop(1).joinToString("/")
    }

    private companion object {
        val DIRECTORY = FileMetadata(isDirectory = true)
        val REGULAR_FILE = FileMetadata(isRegularFile = true)
    }
}

internal object WireErrors {
    private val SYNTAX_ERROR = Regex("""^Syntax error in (.+?:\d+:\d+): (.*)$""", RegexOption.DOT_MATCHES_ALL)
    private val TRAILING_LOCATION = Regex("""\s*\(([^()]+)\)\s*$""")
    private val FILE_CONTEXT = Regex("""^(?:for|in) file (.+)$""")

    /**
     * Splits a [SchemaException] message into `location: message (context)` errors, preferring locations in [sourceDirs].
     */
    fun translate(message: String, sourceDirs: List<File>): List<String> {
        val sourceBases = sourceDirs.map { it.path.replace('\\', '/') + "/" }
        val groups = mutableListOf<MutableList<String>>()
        for (line in message.lines()) {
            if (line.isBlank()) continue
            if (!line.first().isWhitespace() || groups.isEmpty()) groups += mutableListOf(line) else groups.last() += line
        }
        return groups.map { lines ->
            val locations = mutableListOf<String>()
            val context = lines.drop(1).mapNotNull { raw ->
                val line = raw.trim()
                val fileContext = FILE_CONTEXT.find(line)
                val trailing = TRAILING_LOCATION.find(line)
                when {
                    fileContext != null -> {
                        locations += fileContext.groupValues[1]
                        null
                    }

                    trailing != null -> {
                        locations += trailing.groupValues[1]
                        line.removeRange(trailing.range)
                    }

                    else -> line
                }
            }
            val location = locations.firstOrNull { location -> sourceBases.any(location.replace('\\', '/')::startsWith) }
                ?: locations.firstOrNull()
            val suffix = if (context.isEmpty()) "" else " (${context.joinToString(", ")})"
            "${location ?: "<unknown>"}: ${lines.first()}$suffix"
        }
    }

    fun translateSyntaxError(message: String): String {
        val match = SYNTAX_ERROR.find(message) ?: return "<unknown>: $message"
        return "${match.groupValues[1]}: syntax error: ${match.groupValues[2]}"
    }
}
