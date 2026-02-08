package dev.slne.surf.surfapi.gradle.generators.pluginfiles

import org.gradle.api.tasks.Internal

sealed class CommonPluginFile {

    @Internal
    internal abstract fun isApplied(): Boolean

    internal open fun validate() {
    }
}