package dev.slne.surf.nats.gradle.tasks

import java.io.File
import java.util.zip.ZipFile

/**
 * Reads the `.proto` files that surf-nats packs into jars under [PREFIX].
 */
internal object ProtoArchives {
    const val PREFIX = "META-INF/surf-nats/proto/"

    /**
     * Copies the packed protos of [archive], a jar or an exploded jar directory, below the directory [root] returns
     * and returns their number. [root] is called only when the archive packs at least one proto.
     */
    fun extract(archive: File, root: () -> File): Int = when {
        archive.isDirectory -> copyDirectory(archive.resolve(PREFIX), root)
        archive.isFile -> copyJar(archive, root)
        else -> 0
    }

    private fun copyJar(jar: File, root: () -> File): Int {
        ZipFile(jar).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith(PREFIX) && it.name.endsWith(".proto") }
                .sortedBy { it.name }
                .toList()
            if (entries.isEmpty()) return 0
            val target = root()
            for (entry in entries) {
                val file = safeTarget(target, entry.name.removePrefix(PREFIX), jar)
                file.parentFile.mkdirs()
                zip.getInputStream(entry).use { input -> file.outputStream().use(input::copyTo) }
            }
            return entries.size
        }
    }

    private fun copyDirectory(source: File, root: () -> File): Int {
        if (!source.isDirectory) return 0
        val files = source.walkTopDown().filter { it.isFile && it.extension == "proto" }.sortedBy { it.invariantSeparatorsPath }.toList()
        if (files.isEmpty()) return 0
        val target = root()
        for (file in files) {
            val copy = target.resolve(file.relativeTo(source).invariantSeparatorsPath)
            copy.parentFile.mkdirs()
            file.copyTo(copy, overwrite = true)
        }
        return files.size
    }

    private fun safeTarget(root: File, relativePath: String, jar: File): File {
        val base = root.canonicalFile
        val target = base.resolve(relativePath).canonicalFile
        require(target.toPath().startsWith(base.toPath())) { "${jar.name}: entry $PREFIX$relativePath escapes the proto root" }
        return target
    }
}
