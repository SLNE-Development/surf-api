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

    fun mergeWith(other: PluginHookMeta): PluginHookMeta {
        val mergedHooks = ArrayList<Hook>(this.hooks.size + other.hooks.size)
        mergedHooks.addAll(this.hooks)
        for (hook in other.hooks) {
            if (!mergedHooks.any { it.className == hook.className }) {
                mergedHooks.add(hook)
            } else {
                throw IllegalStateException("Duplicate hook className found during merge: ${hook.className}")
            }
        }
        return PluginHookMeta(mergedHooks)
    }

    operator fun plus(other: PluginHookMeta) = mergeWith(other)

    companion object {
        fun empty() = PluginHookMeta(emptyList())
    }
}