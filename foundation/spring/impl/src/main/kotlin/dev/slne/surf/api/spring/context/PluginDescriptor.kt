package dev.slne.surf.api.spring.context

import java.nio.file.Path

/**
 * Describes a plugin for which [SurfContextManager] builds a context.
 *
 * @property name the plugin's name as the platform reports it; the key by which other plugins
 * declare it as a dependency
 * @property classLoader the loader that defined the plugin's classes; the context loads its beans
 * and resolves its resources through it
 * @property source the plugin's jar, or a classes directory in tests; component scanning reads only
 * classes from here, even though [classLoader] also sees the classes of its dependencies. `null`
 * for a plugin that has no jar of its own, such as one shaded into the server: the scan is then not
 * restricted, and reads every class [classLoader] sees under [basePackages].
 * @property basePackages the packages scanned for components
 * @property dependencies the names of the plugins whose exports this plugin may inject: its direct
 * hard dependencies and those soft dependencies that are installed. Names without a context of
 * their own, such as plugins that do not use surf-api, are ignored.
 * @property aliases further names under which other plugins may declare this one as a dependency,
 * such as the names a Paper plugin `provides`
 * @property dataDirectory the folder the platform gives the plugin for its configuration and data,
 * such as `plugins/<name>`; `null` if it gives none. It may not exist yet.
 */
class PluginDescriptor(
    val name: String,
    val classLoader: ClassLoader,
    val source: Path?,
    val basePackages: List<String>,
    val dependencies: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val dataDirectory: Path? = null,
) {
    init {
        require(basePackages.isNotEmpty()) { "Plugin '$name' declares no package to scan" }
    }

    override fun toString(): String = "PluginDescriptor(name=$name, source=$source)"
}
