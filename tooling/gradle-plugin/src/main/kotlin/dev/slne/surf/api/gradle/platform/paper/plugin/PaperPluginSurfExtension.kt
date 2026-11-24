package dev.slne.surf.api.gradle.platform.paper.plugin

import dev.slne.surf.api.gradle.platform.paper.AbstractPaperSurfExtension
import net.minecrell.pluginyml.paper.PaperPluginDescription.DependencyDefinition
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.NamedDomainObjectContainerScope
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class PaperPluginSurfExtension @Inject constructor(objects: ObjectFactory) :
    AbstractPaperSurfExtension(objects) {
    /**
     * The plugin's main class. Absent when the build generates it.
     */
    internal val mainClass = objects.property<String>()

    /**
     * The plugin's bootstrapper. Absent when the build generates it.
     */
    internal val bootstrapper = objects.property<String>()

    /**
     * The surf plugins the plugin depends on, as server and bootstrap dependency alike.
     */
    internal val surfPluginDependencies = objects.listProperty<SurfPluginDependency>()
    internal val bootstrapDependencies =
        objects.property<Action<NamedDomainObjectContainerScope<DependencyDefinition>>>()
    internal val serverDependencies =
        objects.property<Action<NamedDomainObjectContainerScope<DependencyDefinition>>>()
    internal val foliaSupported = objects.property<Boolean>().convention(true)
    internal val generateLibraryLoader = objects.property<Boolean>().convention(false)

    internal val installSurfNpc = objects.property<Boolean>().convention(false)

    /**
     * Uses [mainClass] instead of the generated main class. It must extend `SurfPaperPlugin`.
     */
    fun mainClass(mainClass: String) {
        this.mainClass.set(mainClass)
        this.mainClass.finalizeValue()
    }

    /**
     * Uses [bootstrapper] instead of the generated bootstrapper. It must extend
     * `SurfPaperBootstrapper`.
     */
    fun bootstrapper(bootstrapper: String) {
        this.bootstrapper.set(bootstrapper)
        this.bootstrapper.finalizeValue()
    }

    /**
     * Requires the surf plugin [name] as server and bootstrap dependency, loaded before this
     * plugin.
     *
     * A plugin that depends on a surf plugin's beans needs both: the bootstrap dependency makes
     * the surf plugin's bootstrap context exist when this plugin bootstraps. Use
     * [bootstrapDependencies] and [serverDependencies] for plugins that are not surf plugins.
     */
    fun surfPluginDependency(name: String) {
        surfPluginDependencies.add(SurfPluginDependency(name, required = true))
    }

    /**
     * Like [surfPluginDependency], but the plugin also loads when [name] is missing.
     */
    fun softSurfPluginDependency(name: String) {
        surfPluginDependencies.add(SurfPluginDependency(name, required = false))
    }

    fun bootstrapDependencies(
        action: Action<NamedDomainObjectContainerScope<DependencyDefinition>>,
    ) {
        bootstrapDependencies.set(action)
        bootstrapDependencies.finalizeValue()
    }

    fun serverDependencies(
        action: Action<NamedDomainObjectContainerScope<DependencyDefinition>>,
    ) {
        serverDependencies.set(action)
        serverDependencies.finalizeValue()
    }

    fun foliaSupported(foliaSupported: Boolean) {
        this.foliaSupported.set(foliaSupported)
        this.foliaSupported.finalizeValue()
    }

    fun generateLibraryLoader(generateLibraryLoader: Boolean) {
        this.generateLibraryLoader.set(generateLibraryLoader)
        this.generateLibraryLoader.finalizeValue()
    }

    fun withSurfNpc() {
        installSurfNpc.set(true)
        installSurfNpc.finalizeValue()
    }
}

/**
 * A surf plugin a Paper plugin depends on.
 */
data class SurfPluginDependency(val name: String, val required: Boolean) : java.io.Serializable
