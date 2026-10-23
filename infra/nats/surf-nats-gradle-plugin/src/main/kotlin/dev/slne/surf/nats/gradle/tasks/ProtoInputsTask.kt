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
 * The `.proto` sources and the proto path that the surf-nats tasks read.
 *
 * Subclasses declare the `.proto` files of [sourceDirectories] as their input via [protoFiles].
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
     * Proto roots extracted by [ProtoArchiveTransform] from the jars whose packed protos sources may import.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val protoPathArchives: ConfigurableFileCollection

    /**
     * Returns [protoPathDirectories] followed by [protoPathArchives].
     */
    protected fun protoPath(): List<File> = protoPathDirectories.files.toList() + protoPathArchives.files

    protected fun protoFiles(directories: ConfigurableFileCollection): FileTree = directories.asFileTree.matching { it.include("**/*.proto") }
}
