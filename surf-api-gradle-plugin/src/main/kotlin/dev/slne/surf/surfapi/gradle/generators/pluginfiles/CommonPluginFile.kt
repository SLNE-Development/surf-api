package dev.slne.surf.surfapi.gradle.generators.pluginfiles

import kotlinx.serialization.Serializable
import org.gradle.api.Project

@Serializable
sealed class CommonPluginFile {

    internal open fun setDefaults(project: Project) {

    }

    internal open fun validate() {
    }
}