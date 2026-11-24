package dev.slne.surf.nats.gradle.bridge

import dev.slne.surf.nats.gradle.SurfNatsPluginVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.dependencies

/**
 * Adds the surf-nats contract dependencies to projects that apply a surf-api platform plugin.
 *
 * The surf-nats runtime ships inside the surf-api plugin of each platform, so a project declares no
 * plugin dependency on it.
 */
internal object SurfApiBridge {

    /**
     * Adds the contract dependencies at the version of this plugin, with the Paper types only if
     * [paper] is set.
     *
     * The runtime is bundled into the surf-api plugin of the platform, so the dependencies are
     * `compileOnly`.
     */
    fun Project.addContractDependencies(paper: Boolean) {
        val modules = buildList {
            add("surf-api-nats-api")
            add("surf-api-nats-types-common")

            if (paper) {
                add("surf-api-nats-types-paper")
            }
        }

        val configurations = listOf(
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
            JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
        )

        dependencies {
            for (configuration in configurations) {
                for (module in modules) {
                    configuration("${SurfNatsPluginVersion.GROUP}:$module") {
                        version { this.strictly(SurfNatsPluginVersion.VERSION) }
                    }
                }

                configuration(
                    "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${SurfNatsPluginVersion.KOTLINX_SERIALIZATION_VERSION}",
                )
            }
        }
    }
}
