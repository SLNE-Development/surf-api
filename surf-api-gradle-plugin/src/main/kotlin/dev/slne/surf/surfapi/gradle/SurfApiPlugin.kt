package dev.slne.surf.surfapi.gradle

import dev.slne.surf.surfapi.gradle.DependencyRelocationRegistry.applyRelocations
import dev.slne.surf.surfapi.gradle.PluginRegistry.applyCommonPlugins
import dev.slne.surf.surfapi.gradle.PluginRegistry.applyPlatformDependentPlugins
import dev.slne.surf.surfapi.gradle.ProjectConfigurer.configureKotlin
import dev.slne.surf.surfapi.gradle.RepositoryRegistry.applyRepositories
import dev.slne.surf.surfapi.gradle.extensions.SurfApiExtension
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import dev.slne.surf.surfapi.gradle.platform.config.paper.Paper
import dev.slne.surf.surfapi.gradle.platform.config.paper.applyPaperConfiguration
import dev.slne.surf.surfapi.gradle.platform.config.standalone.Standalone
import dev.slne.surf.surfapi.gradle.platform.config.velocity.Velocity
import dev.slne.surf.surfapi.gradle.platform.config.velocity.applyVelocityConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

@Suppress("unused")
class SurfApiPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        val extension = extensions.create<SurfApiExtension>("surfApi")

        applyRepositories()
        applyCommonPlugins()

        beforeEvaluate {
            configureKotlin()
        }

        afterEvaluate {
            val platform = extension.platform
            require(platform != null) { "No platform specified for Surf API" }
            val configuration = extension.configuration!!

            applyPlatformDependentPlugins(platform)
            dependencies.add(platform.scope, platform.dependency)
            applyRelocations(platform)

            configuration.validate()
            ProjectConfigurer.configureCore(this, configuration as Core)

            when (configuration) {
                is Paper -> applyPaperConfiguration(configuration)
                is Velocity -> applyVelocityConfiguration(configuration)
                is Standalone -> {
                    // Do nothing
                }
            }
        }
    }
}