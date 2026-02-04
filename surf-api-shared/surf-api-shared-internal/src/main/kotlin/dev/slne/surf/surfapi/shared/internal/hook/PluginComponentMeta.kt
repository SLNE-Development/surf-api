package dev.slne.surf.surfapi.shared.internal.hook

import kotlinx.serialization.Serializable

@Serializable
data class PluginComponentMeta(
    val components: List<Component>,
    val postProcessors: List<PostProcessor> = emptyList()
) {

    @Serializable
    data class Component(
        val priority: Short = 0,
        val className: String,
        val classDependencies: List<String> = emptyList(),
        val pluginDependencies: List<String> = emptyList(),
        val pluginOneDependencies: List<List<String>> = emptyList(),
        val componentDependencies: List<String> = emptyList(),
        val customConditions: List<String> = emptyList(),
        val conditionalOnEnvironments: List<List<String>> = emptyList(),
        val conditionalOnMissingComponents: List<String> = emptyList(),
        val conditionalOnProperties: List<PropertyCondition> = emptyList(),
    )

    @Serializable
    data class PropertyCondition(
        val key: Array<String>,
        val havingValue: String = "",
        val matchIfMissing: Boolean = false,
        val file: String = ""
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PropertyCondition

            if (matchIfMissing != other.matchIfMissing) return false
            if (!key.contentEquals(other.key)) return false
            if (havingValue != other.havingValue) return false
            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            var result = matchIfMissing.hashCode()
            result = 31 * result + key.contentHashCode()
            result = 31 * result + havingValue.hashCode()
            result = 31 * result + file.hashCode()
            return result
        }
    }

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

    fun sorted(): PluginComponentMeta {
        val sortedComponents = components.sortedBy { it.priority }
        val sortedPostProcessors = postProcessors.sortedBy { it.priority }
        return PluginComponentMeta(sortedComponents, sortedPostProcessors)
    }

    companion object {
        fun empty() = PluginComponentMeta(emptyList(), emptyList())
    }
}