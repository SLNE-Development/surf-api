package dev.slne.surf.surfapi.shared.internal.hook

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
        val hookDependencies: List<String>,
    )

    companion object {
        fun empty() = PluginHookMeta(emptyList())
    }
}