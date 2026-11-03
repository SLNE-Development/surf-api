package dev.slne.surf.nats.gradle.tasks

import dev.slne.surf.nats.gradle.weaver.WeaverBaselineClient
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration
import dev.slne.surf.nats.gradle.weaver.WeaverVersions
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * Downloads the `.proto` files of this module's previous weaver release into [outputDir].
 *
 * The baseline version is [requestedVersion] when set, otherwise the highest release in [repository] below
 * [projectVersion] without `-SNAPSHOT`. [versionFile] records the version, or [NO_BASELINE] when there is no earlier
 * release, the registry answers 404 for the artifact, or Gradle runs offline. [outputDir] holds only the files of
 * this artifact, not those of its dependencies.
 */
@DisableCachingByDefault(because = "Always re-checks the registry for a newer release")
abstract class FetchSurfNatsBaselineTask : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val url: Property<String>

    @get:Internal
    abstract val token: Property<String>

    @get:Input
    @get:Optional
    abstract val repository: Property<String>

    @get:Input
    @get:Optional
    abstract val artifactGroup: Property<String>

    @get:Input
    @get:Optional
    abstract val artifactName: Property<String>

    @get:Input
    @get:Optional
    abstract val projectVersion: Property<String>

    /**
     * Version from `surfNats.compatibilityBaseline`; replaces the version lookup.
     */
    @get:Input
    @get:Optional
    abstract val requestedVersion: Property<String>

    /**
     * Whether the compatibility check needs a baseline; the task is skipped otherwise.
     */
    @get:Internal
    abstract val active: Property<Boolean>

    /**
     * Whether Gradle runs with `--offline`, captured at configuration time.
     */
    @get:Input
    abstract val offline: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputFile
    abstract val versionFile: RegularFileProperty

    @get:Inject
    protected abstract val archiveOperations: ArchiveOperations

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    init {
        outputs.upToDateWhen { false }
        onlyIf("the compatibility check is enabled and active", Active())
    }

    @TaskAction
    fun fetch() {
        fileSystemOperations.delete { it.delete(outputDir) }
        outputDir.get().asFile.mkdirs()

        if (offline.get()) {
            logger.warn("surf-nats: Gradle runs with --offline, skipping the compatibility check against weaver.")
            writeVersion(NO_BASELINE)
            return
        }

        val repository = required(repository, "weaver.publishing.repository")
        val group = required(artifactGroup, "weaver.publishing.group")
        val name = required(artifactName, "weaver.publishing.name")
        val client = WeaverBaselineClient(required(url, "weaver.url"), token.orNull)

        val version = requestedVersion.orNull ?: findPreviousRelease(client, repository, group, name)
        if (version == null) {
            logNoBaseline(repository, group, name)
            writeVersion(NO_BASELINE)
            return
        }

        val archive = temporaryDir.resolve("baseline.tar.gz")
        try {
            client.download(repository, group, name, version, archive)
            extract(archive, "$group:$name:$version")
        } finally {
            archive.delete()
        }
        writeVersion(version)
        logger.info("surf-nats: compatibility baseline is {}:{}:{}", group, name, version)
    }

    private fun findPreviousRelease(
        client: WeaverBaselineClient,
        repository: String,
        group: String,
        name: String,
    ): String? {
        val releases = client.releases(repository, group, name) ?: return null
        val current = required(projectVersion, "weaver.publishing.version")
        return WeaverVersions.previousRelease(releases, current)
    }

    /**
     * Logs a missing baseline, as a warning outside the public default repository.
     *
     * weaver answers a missing or unreadable repository with the same 404 as a missing artifact.
     */
    private fun logNoBaseline(repository: String, group: String, name: String) {
        val message = "surf-nats: no earlier release of $group:$name in $repository, skipping the compatibility check."
        if (repository == WeaverIntegration.DEFAULT_REPOSITORY) {
            logger.lifecycle(message)
            return
        }

        val cause = if (token.orNull.isNullOrBlank()) {
            "may be private while WEAVER_TOKEN is not set"
        } else {
            "may not exist or not be readable with the configured token"
        }
        logger.warn("$message The repository $cause.")
    }

    private fun extract(archive: File, coordinate: String) {
        fileSystemOperations.copy { spec ->
            spec.from(archiveOperations.tarTree(archiveOperations.gzip(archive)))
            spec.include("**/*.proto")
            spec.includeEmptyDirs = false
            spec.eachFile { details ->
                if (details.relativePath.segments.any { it == ".." }) {
                    throw GradleException("$coordinate: archive entry ${details.path} escapes the baseline directory")
                }
            }
            spec.into(outputDir)
        }
    }

    private fun writeVersion(version: String) {
        val file = versionFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(version, Charsets.UTF_8)
    }

    private fun required(property: Property<String>, name: String): String =
        property.orNull?.takeIf { it.isNotBlank() } ?: throw GradleException("surf-nats: $name is not set.")

    private class Active : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean =
            (element as FetchSurfNatsBaselineTask).active.get()
    }

    companion object {
        /**
         * Content of [versionFile] when there is no baseline to check against.
         */
        const val NO_BASELINE = "none"

        /**
         * Returns the version recorded in [file], or [NO_BASELINE] when the file does not exist.
         */
        fun readVersion(file: File): String {
            return if (file.isFile) file.readText(Charsets.UTF_8).trim() else NO_BASELINE
        }
    }
}
