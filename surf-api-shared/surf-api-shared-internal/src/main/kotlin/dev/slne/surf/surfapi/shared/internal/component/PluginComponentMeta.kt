package dev.slne.surf.surfapi.shared.internal.component

import kotlinx.serialization.Serializable

@Serializable
data class PluginComponentMeta(
    val components: List<Component>,
    val postProcessors: List<PostProcessor> = emptyList()
) {

    @Serializable
    data class Component(
        val priority: Short,
        val className: String,
        val classDependencies: List<String> = emptyList(),
        val pluginDependencies: List<String> = emptyList(),
        val pluginOneDependencies: List<List<String>> = emptyList(),
        val componentDependencies: List<String> = emptyList(),
        val customConditions: List<String> = emptyList(),
    )

    @Serializable
    data class PostProcessor(
        val className: String,
        val priority: Int = 0
    )

    fun mergeWith(other: PluginComponentMeta): PluginComponentMeta {
        val mergedComponents = ArrayList<Component>(this.components.size + other.components.size)
        mergedComponents.addAll(this.components)
        for (component in other.components) {
            if (!mergedComponents.any { it.className == component.className }) {
                mergedComponents.add(component)
            } else {
                throw IllegalStateException("Duplicate component className found during merge: ${component.className}")
            }
        }

        val mergedPostProcessors = ArrayList<PostProcessor>(this.postProcessors.size + other.postProcessors.size)
        mergedPostProcessors.addAll(this.postProcessors)
        for (postProcessor in other.postProcessors) {
            if (!mergedPostProcessors.any { it.className == postProcessor.className }) {
                mergedPostProcessors.add(postProcessor)
            } else {
                throw IllegalStateException("Duplicate post processor className found during merge: ${postProcessor.className}")
            }
        }

        return PluginComponentMeta(mergedComponents, mergedPostProcessors)
    }

    operator fun plus(other: PluginComponentMeta) = mergeWith(other)

    companion object {
        fun empty() = PluginComponentMeta(emptyList(), emptyList())
    }
}
