package dev.slne.surf.surfapi.gradle

import org.gradle.api.Project

object PluginRegistry {
    private val plugins = listOf<String>(
        "java"
    )

    fun applyAll(project: Project) {
        plugins.forEach { pluginId ->
            project.pluginManager.apply(pluginId)
            project.logger.lifecycle("Applied plugin: $pluginId")
        }
    }
}