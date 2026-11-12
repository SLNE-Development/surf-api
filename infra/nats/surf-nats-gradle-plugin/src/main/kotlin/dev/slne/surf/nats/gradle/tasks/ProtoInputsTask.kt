
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
 * Holds the proto source directories and import paths shared by the surf-nats tasks.
 *
 * Only the `.proto` files in these directories count as task inputs. Subclasses declare those of
 * [sourceDirectories] with [protoFiles].
 */
@DisableCachingByDefault(because = "Base class; the concrete surf-nats tasks are cacheable")
abstract class ProtoInputsTask : DefaultTask() {

    /**
     * The directories whose `.proto` files the task processes, from `surfNats.protoSourceDirs`.
     */
    @get:Internal
    abstract val sourceDirectories: ConfigurableFileCollection

    /**
     * The local import directories, from `surfNats.protoPath`.
     */
    @get:Internal
    abstract val protoPathDirectories: ConfigurableFileCollection

    /**
     * The `.proto` files of [protoPathDirectories], declared as task input.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoPathSources: FileTree
        get() = protoFiles(protoPathDirectories)

    /**
     * The proto root that weaver's `fetchProtos` fills (`weaver.protosDir`), used for imports only.
     *
     * Empty when the `weaver` configuration declares no dependencies, so builds without them need no network.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val weaverProtoPath: ConfigurableFileCollection

    /**
     * Returns the configured proto-path directories followed by the weaver proto root.
     */
    protected fun protoPath(): List<File> =
        protoPathDirectories.files.toList() + weaverProtoPath.files

    /**
     * Returns a file tree containing only `.proto` files from [directories].
     */
    protected fun protoFiles(directories: ConfigurableFileCollection): FileTree {
        return directories.asFileTree.matching {
            it.include("**/*.proto")
        }
    }
}
