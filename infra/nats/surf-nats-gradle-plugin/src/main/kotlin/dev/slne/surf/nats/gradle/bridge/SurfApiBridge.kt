package dev.slne.surf.nats.gradle.bridge

import dev.slne.surf.api.gradle.generators.pluginfiles.VelocityPluginFile
import dev.slne.surf.nats.gradle.SurfNatsPluginVersion
import net.minecrell.pluginyml.paper.PaperPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.JavaPlugin

/**
 * Adds the surf-nats dependencies of the surf-api platform plugins.
 *
 * Loaded only from `withPlugin` callbacks of surf-api plugin ids, so projects without surf-api never load surf-api
 * or plugin-yml classes.
 */
internal object SurfApiBridge {
    private const val PAPER_PLUGIN = "surf-nats-paper"
    private const val VELOCITY_PLUGIN = "surf-nats-velocity"

    /**
     * Adds the contract dependencies at the version of this plugin, with the Paper types only if [paper] is set.
     *
     * The runtime comes from the surf-nats platform plugin, so the dependencies are `compileOnly`.
     */
    fun addContractDependencies(project: Project, paper: Boolean) {
        val modules = buildList {
            add("surf-nats-api")
            add("surf-nats-types-common")
            if (paper) add("surf-nats-types-paper")
        }
        for (configuration in listOf(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)) {
            for (module in modules) {
                val dependency = project.dependencies.create("${SurfNatsPluginVersion.GROUP}:$module") as ExternalModuleDependency
                dependency.version { it.strictly(SurfNatsPluginVersion.VERSION) }
                project.dependencies.add(configuration, dependency)
            }
            project.dependencies.add(
                configuration,
                "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${SurfNatsPluginVersion.KOTLINX_SERIALIZATION_VERSION}",
            )
        }
    }

    /**
     * Declares `surf-nats-paper` as a required bootstrap and server dependency in the `paper-plugin.yml`.
     */
    fun requirePaperPlugin(project: Project) {
        val description = project.extensions.getByType(PaperPluginDescription::class.java)
        for (dependencies in listOf(description.bootstrapDependencies, description.serverDependencies)) {
            dependencies.register(PAPER_PLUGIN) { dependency ->
                dependency.required = true
                dependency.joinClasspath = true
                dependency.load = RelativeLoadOrder.BEFORE
            }
        }
    }

    /**
     * Declares `surf-nats-velocity` as a required dependency in the `velocity-plugin.json`.
     */
    fun requireVelocityPlugin(project: Project) {
        project.extensions.getByType(VelocityPluginFile::class.java).pluginDependencies.register(VELOCITY_PLUGIN)
    }
}
