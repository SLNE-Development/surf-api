package dev.slne.surf.nats.gradle.bridge

import dev.slne.surf.api.gradle.platform.common.CommonSurfExtension
import dev.slne.surf.nats.gradle.SurfNatsPluginVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.JavaPlugin

/**
 * Connects the plugin to a surf-api platform plugin until surf-api drops `withSurfNats()`.
 *
 * Loaded only from `withPlugin` callbacks of surf-api plugin ids, so projects without surf-api never load
 * [CommonSurfExtension].
 */
internal object SurfApiBridge {
    fun configure(project: Project, paper: Boolean) {
        val extension = project.extensions.getByType(CommonSurfExtension::class.java)
        if (!extension.surfNatsEnabled.get()) extension.withSurfNats()

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
}
