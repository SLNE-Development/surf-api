@file:Suppress("DEPRECATION")

package dev.slne.surf.microservice.launcher

import dev.slne.surf.surfapi.core.api.util.logger
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.supplier.RepositorySystemSupplier
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.TransferEvent
import java.nio.file.Path

class MicroserviceLibraryLoader {
    private val repository = RepositorySystemSupplier().repositorySystem
    private val session = MavenRepositorySystemUtils.newSession().apply {
        setSystemProperties(System.getProperties())
        setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL)
        setLocalRepositoryManager(
            repository.newLocalRepositoryManager(
                this,
                LocalRepository("libraries")
            )
        )
        setReadOnly()

        setTransferListener(object : AbstractTransferListener() {
            override fun transferInitiated(event: TransferEvent) {
                log.atInfo()
                    .log("Transfer initiated for ${event.resource.repositoryUrl}${event.resource.resourceName}")
            }

            override fun transferFailed(event: TransferEvent) {
                log.atSevere()
                    .log("Transfer failed for ${event.resource.repositoryUrl}${event.resource.resourceName}: ${event.exception.message}")
            }

            override fun transferSucceeded(event: TransferEvent) {
                log.atInfo()
                    .log("Transfer succeeded for ${event.resource.repositoryUrl}${event.resource.resourceName}")
            }

            override fun transferCorrupted(event: TransferEvent) {
                log.atWarning()
                    .log("Transfer corrupted for ${event.resource.repositoryUrl}${event.resource.resourceName}: ${event.exception.message}")
            }

            override fun transferStarted(event: TransferEvent) {
                log.atInfo()
                    .log("Transfer started for ${event.resource.repositoryUrl}${event.resource.resourceName}")
            }
        })
    }

    fun loadLibraries(dependencies: List<String>): List<Path> {
        val dependencies = dependencies.filter { !it.isEmpty() }
            .map { dependencyString ->
                val parts = dependencyString.split(":", limit = 3)

                if (parts.size != 3) {
                    throw IllegalArgumentException("Invalid dependency format: $dependencyString. Expected format is group:artifact:version")
                }

                val (groupId, artifactId, version) = parts

                Dependency(
                    DefaultArtifact("%s%s%s".format(groupId, artifactId, version)),
                    null
                )
            }

        val collectRequest = CollectRequest().apply {
            setDependencies(dependencies)
            setRepositories(listOf(SLNE_PUBLIC, MAVEN_CENTRAL))
        }

        val request = DependencyRequest(collectRequest, null)
        val result = repository.resolveDependencies(session, request)

        return result.artifactResults.map { it.artifact.path }
    }

    companion object {
        private val log = logger()

        private val SLNE_PUBLIC = RemoteRepository.Builder(
            "slne-public",
            "default",
            "https://repo.slne.dev/repository/maven-public/"
        ).setPolicy(
            RepositoryPolicy(
                true,
                RepositoryPolicy.CHECKSUM_POLICY_WARN,
                RepositoryPolicy.UPDATE_POLICY_ALWAYS
            )
        ).build()

        private val MAVEN_CENTRAL = RemoteRepository.Builder(
            "maven-central",
            "default",
            "https://repo1.maven.org/maven2/"
        ).setPolicy(
            RepositoryPolicy(
                true,
                RepositoryPolicy.CHECKSUM_POLICY_WARN,
                RepositoryPolicy.UPDATE_POLICY_ALWAYS
            )
        ).build()
    }
}