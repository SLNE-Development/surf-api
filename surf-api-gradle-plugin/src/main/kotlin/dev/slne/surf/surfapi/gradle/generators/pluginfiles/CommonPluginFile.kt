package dev.slne.surf.surfapi.gradle.generators.pluginfiles

import kotlinx.serialization.Serializable
import org.gradle.api.Project
import org.gradle.api.tasks.Internal

@Serializable
sealed class CommonPluginFile {

    @Internal
    internal abstract fun isApplied(): Boolean

    internal open fun setDefaults(project: Project) {

    }

    internal open fun validate() {
    }
}