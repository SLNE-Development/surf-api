package dev.slne.surf.surfapi.processor.hook

import kotlinx.serialization.Serializable

@Serializable
data class PluginHookMeta(val hooks: List<Hook>) {

    @Serializable
    data class Hook(
        val priority: Short,
        val className: String,
        val classDependencies: List<String>,
        val pluginDependencies: List<String>,
        val pluginOneDependencies: List<List<String>>,
    )

    companion object {
        fun empty() = PluginHookMeta(emptyList())
    }
}