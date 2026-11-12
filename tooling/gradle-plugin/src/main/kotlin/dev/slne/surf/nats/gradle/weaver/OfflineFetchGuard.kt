package dev.slne.surf.nats.gradle.weaver

import dev.slne.weaver.gradle.FetchProtosTask
import kotlinx.serialization.json.*
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task
import java.io.File

/**
 * Fails `fetchProtos` with a clear message when Gradle runs offline and remote weaver dependencies must be resolved.
 *
 * Remote roots are the task's own external dependencies and the `external` lists of the local module manifests
 * (`{"coordinate": ..., "external": [...]}`). Like weaver's own resolution, roots whose `group:name` a local module
 * publishes are substituted by that module and need no network. Builds whose weaver dependencies are all local
 * projects pass offline.
 */
internal class OfflineFetchGuard(private val offline: Boolean) : Action<Task> {

    override fun execute(task: Task) {
        if (!offline) return

        val fetch = task as FetchProtosTask
        val manifests = fetch.localManifests.files.filter(File::isFile).map(::readManifest)
        val local = manifests.mapNotNullTo(HashSet()) { it.coordinate?.substringBeforeLast(':') }
        val remote = (fetch.dependencies.get() + manifests.flatMap { it.external })
            .distinct()
            .filterNot { it.substringBeforeLast(':') in local }
        if (remote.isEmpty()) return

        throw GradleException(
            "surf-nats: weaver dependencies ${remote.joinToString()} need the network, but Gradle runs with --offline. " +
                    "fetchProtos resolves them against the registry on every run; build without --offline."
        )
    }

    private fun readManifest(manifest: File): Manifest =
        try {
            val json = Json.parseToJsonElement(manifest.readText()).jsonObject
            Manifest(
                coordinate = (json["coordinate"] as? JsonPrimitive)?.contentOrNull,
                external = json["external"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
            )
        } catch (e: RuntimeException) {
            throw GradleException("Invalid weaver module manifest $manifest: ${e.message}", e)
        }

    private class Manifest(val coordinate: String?, val external: List<String>)
}
