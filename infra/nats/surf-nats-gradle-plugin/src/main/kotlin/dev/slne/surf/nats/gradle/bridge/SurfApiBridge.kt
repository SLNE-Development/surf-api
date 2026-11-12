package dev.slne.surf.nats.gradle.bridge

import dev.slne.surf.api.gradle.generators.pluginfiles.VelocityPluginFile
import dev.slne.surf.nats.gradle.SurfNatsPluginVersion
import net.minecrell.pluginyml.paper.PaperPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

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
    fun Project.addContractDependencies(paper: Boolean) {
        val modules = buildList {
            add("surf-nats-api")
            add("surf-nats-types-common")
            if (paper) add("surf-nats-types-paper")
        }

        val configurations = listOf(
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
            JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
        )

        dependencies {
            for (configuration in configurations) {
                for (module in modules) {
                    configuration("${SurfNatsPluginVersion.GROUP}:$module") {
                        version { it.strictly(SurfNatsPluginVersion.VERSION) }
                    }
                }

                configuration("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${SurfNatsPluginVersion.KOTLINX_SERIALIZATION_VERSION}")
            }
        }
    }

    /**
     * Declares `surf-nats-paper` as a required bootstrap and server dependency in the `paper-plugin.yml`.
     */
    fun Project.requirePaperPlugin() = configure<PaperPluginDescription> {
        listOf(bootstrapDependencies, serverDependencies).forEach { deps ->
            deps.register(PAPER_PLUGIN) { dep ->
                dep.required = true
                dep.joinClasspath = true
                dep.load = RelativeLoadOrder.BEFORE
            }
        }
    }

    /**
     * Declares `surf-nats-velocity` as a required dependency in the `velocity-plugin.json`.
     */
    fun Project.requireVelocityPlugin() {
        configure<VelocityPluginFile> {
            pluginDependencies.register(VELOCITY_PLUGIN) { dep ->
                dep.optional.set(false)
            }
        }
    }
}
