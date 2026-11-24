package dev.slne.surf.api.spring.context

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource

/**
 * Loads a plugin context's resources, including those its class loader can load classes from but
 * does not hand out as resources.
 *
 * Paper's plugin class loader resolves classes through the plugins it joins, yet `getResource` only
 * searches the plugin's own jar. Spring reads the bytecode of every interface and superclass of a
 * configuration class as a `.class` resource, so a bean implementing a surf-api interface would
 * fail the refresh. For a missing `.class` resource this loader asks the class loader that defined
 * the class. Every other resource resolves exactly as the plugin's class loader resolves it, so
 * another jar's configuration files never appear as the plugin's own.
 */
internal class PluginResourceLoader(
    private val pluginClassLoader: ClassLoader,
) : DefaultResourceLoader(pluginClassLoader) {

    override fun getResource(location: String): Resource {
        val resource = super.getResource(location)
        if (!location.startsWith(CLASSPATH_URL_PREFIX) || resource.exists()) return resource

        val path = location.removePrefix(CLASSPATH_URL_PREFIX).removePrefix("/")

        return definingLoaderResource(path) ?: resource
    }

    private fun definingLoaderResource(path: String): Resource? {
        if (!path.endsWith(CLASS_SUFFIX)) return null

        val binaryName = path.removeSuffix(CLASS_SUFFIX).replace('/', '.')
        val definingLoader = runCatching { Class.forName(binaryName, false, pluginClassLoader) }
            .getOrNull()
            ?.classLoader
            ?: return null

        return ClassPathResource(path, definingLoader).takeIf(Resource::exists)
    }

    private companion object {
        const val CLASS_SUFFIX = ".class"
    }
}
