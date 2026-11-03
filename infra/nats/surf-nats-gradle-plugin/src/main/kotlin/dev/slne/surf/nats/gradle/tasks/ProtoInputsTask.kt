
package dev.slne.surf.nats.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Provides the proto source directories and import paths shared by surf-nats tasks.
 *
 * Subclasses use [protoFiles] to declare the proto files in [sourceDirectories]
 * as Gradle task inputs.
 */
@DisableCachingByDefault(because = "Base class; the concrete surf-nats tasks are cacheable")
abstract class ProtoInputsTask : DefaultTask() {

    @get:Internal
    abstract val sourceDirectories: ConfigurableFileCollection

    @get:Internal
    abstract val protoPathDirectories: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoPathSources: FileTree
        get() = protoFiles(protoPathDirectories)

    /**
     * Proto roots extracted from dependency JARs by [ProtoArchiveTransform].
     *
     * These roots provide additional proto files that source files may import.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val protoPathArchives: ConfigurableFileCollection

    /**
     * Returns the configured proto-path directories followed by the extracted
     * proto roots from dependency archives.
     */
    protected fun protoPath(): List<File> =
        protoPathDirectories.files.toList() + protoPathArchives.files

    /**
     * Returns a file tree containing only `.proto` files from [directories].
     */
    protected fun protoFiles(directories: ConfigurableFileCollection): FileTree {
        return directories.asFileTree.matching {
            it.include("**/*.proto")
        }
    }
}
