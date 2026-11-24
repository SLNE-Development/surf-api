package dev.slne.surf.api.spring.context

import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourcePatternResolver
import java.net.URI
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath

/**
 * Resolves resource patterns through [delegate] but keeps only resources that come from the
 * plugin's own jar or classes directory.
 *
 * A plugin's class loader also sees the classes of its dependencies and of surf-api. Without this
 * filter, scanning a package prefix that a dependency shares, such as `dev.slne.surf`, would
 * register that dependency's components a second time in the plugin's context. Single resources
 * resolve unfiltered, so the scanner can still read the bytecode of supertypes that live elsewhere.
 */
internal class PluginSourceResourceResolver(
    private val delegate: ResourcePatternResolver,
    source: Path,
) : ResourcePatternResolver by delegate {

    private val source = source.toAbsolutePath().normalize()
    private val sourceIsDirectory = this.source.isDirectory()

    override fun getResources(locationPattern: String): Array<Resource> {
        return delegate.getResources(locationPattern)
            .filter(::belongsToSource)
            .toTypedArray()
    }

    private fun belongsToSource(resource: Resource): Boolean {
        val location = runCatching { resource.url }.getOrNull() ?: return false
        val path = containingPath(location) ?: return false

        return if (sourceIsDirectory) path.startsWith(source) else path == source
    }

    /**
     * The file a resource lives in: the jar for a `jar:` URL, the file itself for a `file:` URL.
     */
    private fun containingPath(location: URL): Path? {
        return runCatching {
            when (location.protocol) {
                "jar" -> URI(location.path.substringBefore("!/")).toPath()
                "file" -> location.toURI().toPath()
                else -> null
            }?.toAbsolutePath()?.normalize()
        }.getOrNull()
    }
}
