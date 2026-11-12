package dev.slne.surf.nats.gradle.schema

import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.generator.SurfNatsGeneratorException
import okio.*
import java.io.File

/**
 * A linked Wire schema and its source proto files.
 *
 * [sourceFiles] contains only files from the source roots, sorted by
 * relative path and then by root.
 */
class LoadedSchema(
    val schema: Schema,
    val sourceFiles: List<ProtoFile>,
)

/**
 * Loads proto files from [sourceDirs], resolving imports against the bundled
 * protos and [protoPathDirs].
 *
 * Source files that conflict with bundled protos are rejected. Copies of
 * bundled protos in proto-path directories are hidden from the loader.
 *
 * @throws SurfNatsGeneratorException if validation or schema loading fails.
 */
fun loadSchema(sourceDirs: List<File>, protoPathDirs: List<File>): LoadedSchema {
    val sourceRoots = existingDirectories(sourceDirs)
    val protoPathRoots = existingDirectories(protoPathDirs)

    validateSourceRoots(sourceDirs, sourceRoots)

    val loader = createSchemaLoader(sourceRoots, protoPathRoots)
    val schema = loadWireSchema(loader, sourceRoots)
    val sourceFiles = selectSourceFiles(schema, loader)

    return LoadedSchema(schema, sourceFiles)
}

private fun existingDirectories(directories: List<File>): List<File> = directories
    .filter(File::isDirectory)
    .map(::normalized)
    .distinct()

private fun validateSourceRoots(
    requestedDirs: List<File>,
    sourceRoots: List<File>,
) {
    if (sourceRoots.none { it.containsProtos() }) {
        throw SurfNatsGeneratorException(
            listOf("${requestedDirs.joinToString()}: no .proto files found")
        )
    }

    val conflicts = sourceRoots.flatMap { root ->
        BundledProtos.paths
            .filter { root.resolve(it).isFile }
            .map { relativePath ->
                val location = Location.get(root.path, relativePath)
                "$location: $relativePath is bundled with the surf-nats plugin and must not be a proto source"
            }
    }

    if (conflicts.isNotEmpty()) {
        throw SurfNatsGeneratorException(conflicts)
    }
}

private fun createSchemaLoader(
    sourceRoots: List<File>,
    protoPathRoots: List<File>,
): SchemaLoader {
    val hiddenPaths = protoPathRoots.flatMapTo(HashSet()) { root ->
        BundledProtos.paths.map { normalized(root.resolve(it)) }
    }

    val loader = SchemaLoader(ProtoFileSystem(hiddenPaths))
    loader.initRoots(
        sourcePath = sourceRoots.map { Location.get(it.path) },
        protoPath = listOf(Location.get(BundledProtos.ROOT)) +
                protoPathRoots.map { Location.get(it.path) },
    )

    return loader
}

private fun loadWireSchema(
    loader: SchemaLoader,
    sourceRoots: List<File>,
): Schema {
    return try {
        loader.loadSchema()
    } catch (e: SchemaException) {
        throw SurfNatsGeneratorException(
            WireErrors.translate(e.message.orEmpty(), sourceRoots)
        )
    } catch (e: IllegalStateException) {
        throw SurfNatsGeneratorException(
            listOf(WireErrors.translateSyntaxError(e.message.orEmpty()))
        )
    }
}

private fun selectSourceFiles(
    schema: Schema,
    loader: SchemaLoader,
): List<ProtoFile> {
    val sourceLocations = loader.sourcePathFiles.mapTo(HashSet()) {
        it.location.base to it.location.path
    }

    return schema.protoFiles
        .filter { (it.location.base to it.location.path) in sourceLocations }
        .sortedWith(compareBy({ it.location.path }, { it.location.base }))
}

private fun normalized(file: File): File = file.absoluteFile.normalize()

/**
 * Provides bundled protos from the plugin classpath under [BundledProtos.ROOT]
 * and hides proto-path files that duplicate bundled paths.
 *
 * Source and proto-path roots are absolute, so they cannot conflict with
 * the relative bundled root.
 */
private class ProtoFileSystem(
    private val hiddenPaths: Set<File>,
) : ForwardingFileSystem(SYSTEM) {

    override fun metadataOrNull(path: Path): FileMetadata? {
        val bundledPath = bundledPath(path)
        if (bundledPath != null) {
            return when {
                bundledPath.isEmpty() -> DIRECTORY
                bundledPath in BundledProtos.paths -> REGULAR_FILE
                else -> null
            }
        }

        if (hiddenPaths.isNotEmpty() && normalized(path.toFile()) in hiddenPaths) {
            return null
        }

        return super.metadataOrNull(path)
    }

    override fun source(file: Path): Source {
        val bundledPath = bundledPath(file) ?: return super.source(file)
        return BundledProtos.open(bundledPath).source()
    }

    /**
     * Returns the path relative to [BundledProtos.ROOT], or `null` if the
     * path is outside the bundled root. The root itself returns an empty string.
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

/**
 * Converts Wire schema errors into diagnostics with source locations.
 */
internal object WireErrors {

    private val SYNTAX_ERROR = Regex(
        """^Syntax error in (.+?:\d+:\d+): (.*)$""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val TRAILING_LOCATION = Regex("""\s*\(([^()]+)\)\s*$""")
    private val FILE_CONTEXT = Regex("""^(?:for|in) file (.+)$""")

    /**
     * Converts a [SchemaException] message into individual diagnostics.
     *
     * Each diagnostic follows the format `location: message (context)`.
     * When multiple locations are present, locations within [sourceDirs]
     * take precedence.
     */
    fun translate(message: String, sourceDirs: List<File>): List<String> {
        val sourceBases = sourceDirs.map {
            it.path.replace('\\', '/') + "/"
        }

        return groupErrorLines(message).map { lines ->
            translateErrorGroup(lines, sourceBases)
        }
    }

    /**
     * Converts a Wire syntax error into a location-prefixed diagnostic.
     */
    fun translateSyntaxError(message: String): String {
        val match = SYNTAX_ERROR.find(message)
            ?: return "<unknown>: $message"

        return "${match.groupValues[1]}: syntax error: ${match.groupValues[2]}"
    }

    private fun groupErrorLines(message: String): List<List<String>> {
        val groups = mutableListOf<MutableList<String>>()

        for (line in message.lines()) {
            if (line.isBlank()) continue

            if (groups.isEmpty() || !line.first().isWhitespace()) {
                groups.add(mutableListOf(line))
            } else {
                groups.last().add(line)
            }
        }

        return groups
    }

    private fun translateErrorGroup(
        lines: List<String>,
        sourceBases: List<String>,
    ): String {
        val locations = mutableListOf<String>()
        val contextLines = extractContextLines(lines, locations)
        val location = preferredLocation(locations, sourceBases)

        val suffix = if (contextLines.isEmpty()) {
            ""
        } else {
            " (${contextLines.joinToString(", ")})"
        }

        return "${location ?: "<unknown>"}: ${lines.first()}$suffix"
    }

    private fun extractContextLines(
        lines: List<String>,
        locations: MutableList<String>,
    ): List<String> {
        return lines.drop(1).mapNotNull { rawLine ->
            val line = rawLine.trim()
            val fileContext = FILE_CONTEXT.find(line)
            val trailingLocation = TRAILING_LOCATION.find(line)

            when {
                fileContext != null -> {
                    locations.add(fileContext.groupValues[1])
                    null
                }

                trailingLocation != null -> {
                    locations.add(trailingLocation.groupValues[1])
                    line.removeRange(trailingLocation.range)
                }

                else -> line
            }
        }
    }

    private fun preferredLocation(
        locations: List<String>,
        sourceBases: List<String>,
    ): String? {
        return locations.firstOrNull { location ->
            val normalizedLocation = location.replace('\\', '/')
            sourceBases.any(normalizedLocation::startsWith)
        } ?: locations.firstOrNull()
    }
}
