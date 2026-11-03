
package dev.slne.surf.nats.gradle.tasks

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Extracts proto files packaged in JARs under [PREFIX].
 *
 * Supports both JAR archives and exploded JAR directories.
 */
internal object ProtoArchives {

    const val PREFIX = "META-INF/surf-nats/proto/"

    /**
     * Extracts the proto files from [archive] into the directory provided by [root].
     *
     * Supports JAR files and exploded JAR directories. Returns the number of
     * extracted files, or zero if no proto files are found.
     *
     * [root] is invoked only when at least one proto file is available.
     */
    fun extract(archive: File, root: () -> File): Int = when {
        archive.isDirectory -> copyDirectory(archive.resolve(PREFIX), root)
        archive.isFile -> copyJar(archive, root)
        else -> 0
    }

    private fun copyJar(jar: File, root: () -> File): Int {
        ZipFile(jar).use { zip ->
            val entries = findProtoEntries(zip)
            if (entries.isEmpty()) return 0

            val targetRoot = root()

            for (entry in entries) {
                val relativePath = entry.name.removePrefix(PREFIX)
                val targetFile = safeTarget(targetRoot, relativePath, jar)

                targetFile.parentFile.mkdirs()

                zip.getInputStream(entry).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            return entries.size
        }
    }

    private fun findProtoEntries(zip: ZipFile): List<ZipEntry> =
        zip.entries().asSequence()
            .filter { entry ->
                !entry.isDirectory &&
                        entry.name.startsWith(PREFIX) &&
                        entry.name.endsWith(".proto")
            }
            .sortedBy { it.name }
            .toList()

    private fun copyDirectory(source: File, root: () -> File): Int {
        if (!source.isDirectory) return 0

        val protoFiles = findProtoFiles(source)
        if (protoFiles.isEmpty()) return 0

        val targetRoot = root()

        for (sourceFile in protoFiles) {
            val relativePath = sourceFile.relativeTo(source).invariantSeparatorsPath
            val targetFile = targetRoot.resolve(relativePath)

            targetFile.parentFile.mkdirs()
            sourceFile.copyTo(targetFile, overwrite = true)
        }

        return protoFiles.size
    }

    private fun findProtoFiles(directory: File): List<File> =
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "proto" }
            .sortedBy { it.invariantSeparatorsPath }
            .toList()

    /**
     * Resolves a JAR entry relative to [root], rejecting paths that escape
     * the destination directory.
     *
     * Both paths are canonicalized to account for traversal and symbolic links.
     *
     * @throws IllegalArgumentException if the entry escapes the proto root.
     */
    private fun safeTarget(root: File, relativePath: String, jar: File): File {
        val base = root.canonicalFile
        val target = base.resolve(relativePath).canonicalFile

        require(target.toPath().startsWith(base.toPath())) {
            "${jar.name}: entry $PREFIX$relativePath escapes the proto root"
        }

        return target
    }
}
