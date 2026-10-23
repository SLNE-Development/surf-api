package dev.slne.surf.nats.gradle.tasks

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Extracts the `.proto` files a jar or exploded jar directory packs under [ProtoArchives.PREFIX] into a proto root.
 *
 * Archives without packed protos produce no output and drop out of the resolved files.
 */
@CacheableTransform
abstract class ProtoArchiveTransform : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val archive: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val archive = archive.get().asFile
        ProtoArchives.extract(archive) { outputs.dir(archive.nameWithoutExtension) }
    }

    companion object {
        /**
         * Artifact type of the extracted proto roots.
         */
        const val PROTO_ROOT_TYPE = "surf-nats-proto-root"
    }
}
