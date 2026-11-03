
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
 * Extracts proto files from JAR archives or exploded JAR directories.
 *
 * Only files under [ProtoArchives.PREFIX] are extracted. Archives without
 * proto files produce no output and are excluded from the resolved artifacts.
 */
@CacheableTransform
abstract class ProtoArchiveTransform : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val archive: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val archiveFile = archive.get().asFile

        ProtoArchives.extract(archiveFile) {
            outputs.dir(archiveFile.nameWithoutExtension)
        }
    }

    companion object {
        /**
         * Artifact type assigned to extracted proto roots.
         */
        const val PROTO_ROOT_TYPE = "surf-nats-proto-root"
    }
}
