package dev.slne.surf.surfapi.shared.internal.hook

import kotlinx.serialization.Serializable

@Serializable
data class PluginHookMeta(val hooks: List<Hook>) {

    @Serializable
    data class Hook(
        val priority: Short,
        val className: String,
        val classDependencies: List<String> = emptyList(),
        val pluginDependencies: List<String> = emptyList(),
        val pluginOneDependencies: List<List<String>> = emptyList(),
        val hookDependencies: List<String> = emptyList(),
        val customConditions: List<String> = emptyList(),
    )

    companion object {
        fun empty() = PluginHookMeta(emptyList())
    }
}