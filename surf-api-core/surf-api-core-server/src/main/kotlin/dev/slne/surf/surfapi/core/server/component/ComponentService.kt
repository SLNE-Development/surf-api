package dev.slne.surf.surfapi.core.server.component

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asLoadingCache
import dev.slne.surf.surfapi.core.api.util.*
import dev.slne.surf.surfapi.shared.api.component.Component
import dev.slne.surf.surfapi.shared.api.component.condition.ComponentCondition
import dev.slne.surf.surfapi.shared.api.component.condition.ComponentConditionContext
import dev.slne.surf.surfapi.shared.api.component.processor.ComponentContext
import dev.slne.surf.surfapi.shared.api.component.processor.ComponentPostProcessor
import dev.slne.surf.surfapi.shared.internal.component.ComponentsConfig
import dev.slne.surf.surfapi.shared.internal.component.PluginComponentMeta
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.*
import java.util.jar.JarFile

abstract class ComponentService {

    private val componentMetaCache = Caffeine.newBuilder()
        .weakKeys()
        .build<Any, PluginComponentMeta> { owner -> loadComponentsMeta(owner) }

    private val componentsCache = Caffeine.newBuilder()
        .weakKeys()
        .asLoadingCache { owner -> loadComponents(owner) }

    private val postProcessorsCache = Caffeine.newBuilder()
        .weakKeys()
        .asLoadingCache { owner -> loadPostProcessors(owner) }

    private fun loadComponentsMeta(owner: Any): PluginComponentMeta {
        val classloader = getClassloader(owner)
        val logger = getLogger(owner)
        var meta = PluginComponentMeta.empty()

        try {
            val resources = classloader.getResources(ComponentsConfig.COMPONENTS_DIRECTORY)
            while (resources.hasMoreElements()) {
                val url = resources.nextElement()

                if (url.protocol == "jar") {
                    val jarPath = url.path.substringBefore("!")
                    val jarFile = JarFile(File(URI(jarPath)))
                    jarFile.entries().asSequence()
                        .filter { it.name.startsWith(ComponentsConfig.COMPONENTS_DIRECTORY) && it.name.endsWith(".json") }
                        .forEach { entry ->
                            try {
                                val raw = jarFile.getInputStream(entry).bufferedReader().use { it.readText() }
                                val decoded = ComponentsConfig.json.decodeFromString<PluginComponentMeta>(raw)
                                meta += decoded
                            } catch (e: Exception) {
                                logger.error("Failed to parse ${entry.name}", e)
                            }
                        }
                } else {
                    val dir = File(url.toURI())
                    dir.listFiles { file -> file.extension == "json" }?.forEach { file ->
                        try {
                            val raw = file.readText()
                            val decoded = ComponentsConfig.json.decodeFromString<PluginComponentMeta>(raw)
                            meta += decoded
                        } catch (e: Exception) {
                            logger.error("Failed to parse ${file.name}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("No components directory found or error reading components", e)
        }

        return meta
    }

    private suspend fun loadComponents(owner: Any): List<Component> {
        val meta = componentMetaCache.get(owner)
        val classLoader = getClassloader(owner)

        val componentsWithMeta = meta.components.mapNotNull { componentMeta ->
            val component = instantiateComponentIfValid(owner, componentMeta, classLoader)
            if (component != null) {
                componentMeta to component
            } else {
                null
            }
        }

        val sortedComponents = topologicalSort(componentsWithMeta, owner)
        
        // Load and apply post-processors
        val postProcessors = postProcessorsCache.get(owner)
        return applyPostProcessors(sortedComponents, postProcessors, owner)
    }

    private suspend fun loadPostProcessors(owner: Any): List<ComponentPostProcessor> {
        val meta = componentMetaCache.get(owner)
        val classLoader = getClassloader(owner)
        val logger = getLogger(owner)

        return meta.postProcessors
            .mapNotNull { postProcessorMeta ->
                try {
                    val clazz = Class.forName(postProcessorMeta.className, false, classLoader)
                    val kClass = clazz.kotlin
                    val objectInstance = kClass.objectInstance
                    if (objectInstance != null) {
                        require(objectInstance is ComponentPostProcessor) { "Post processor must implement ComponentPostProcessor" }
                        objectInstance
                    } else {
                        val constructor = clazz.getConstructor()
                        val instance = constructor.newInstance()
                        require(instance is ComponentPostProcessor) { "Post processor must implement ComponentPostProcessor" }
                        instance
                    }
                } catch (e: Exception) {
                    logger.error("Failed to load post processor ${postProcessorMeta.className}", e)
                    null
                }
            }
            .sortedBy { it.priority }
    }

    private suspend fun applyPostProcessors(
        components: List<Component>,
        postProcessors: List<ComponentPostProcessor>,
        owner: Any
    ): List<Component> {
        if (postProcessors.isEmpty()) {
            return components
        }

        val context = ComponentContext(
            owner = owner,
            allComponents = components
        )

        return components.map { component ->
            var processedComponent = component
            for (processor in postProcessors) {
                processedComponent = processor.postProcessAfterInitialization(
                    processedComponent,
                    processedComponent::class.qualifiedName ?: processedComponent::class.java.name,
                    context
                )
            }
            processedComponent
        }
    }

    suspend fun invokePostProcessorsBeforeDestruction(owner: Any) {
        val components = componentsCache.underlying().asMap()[owner]?.getNow(emptyList()) ?: return
        val postProcessors = postProcessorsCache.underlying().asMap()[owner]?.getNow(emptyList()) ?: return
        
        if (postProcessors.isEmpty() || components.isEmpty()) {
            return
        }

        val context = ComponentContext(
            owner = owner,
            allComponents = components
        )

        // Invoke in reverse priority order
        val reversedProcessors = postProcessors.reversed()
        for (component in components.reversed()) {
            for (processor in reversedProcessors) {
                processor.postProcessBeforeDestruction(
                    component,
                    component::class.qualifiedName ?: component::class.java.name,
                    context
                )
            }
        }
    }

    private suspend fun instantiateComponentIfValid(
        owner: Any,
        componentMeta: PluginComponentMeta.Component,
        classLoader: ClassLoader
    ): Component? {
        val missingDependencies = mutableObject2ObjectMapOf<String, MutableSet<String>>()
        for (classDependency in componentMeta.classDependencies) {
            try {
                Class.forName(classDependency, false, classLoader)
            } catch (_: ClassNotFoundException) {
                missingDependencies.computeIfAbsent("Class") { mutableObjectSetOf() }.add(classDependency)
            }
        }

        for (pluginDependencyId in componentMeta.pluginDependencies) {
            if (!isPluginLoaded(pluginDependencyId)) {
                missingDependencies.computeIfAbsent("Plugin") { mutableObjectSetOf() }.add(pluginDependencyId)
            }
        }

        for (pluginDependenciesIds in componentMeta.pluginOneDependencies) {
            if (pluginDependenciesIds.none { isPluginLoaded(it) }) {
                missingDependencies.computeIfAbsent("Plugin (one of)") { mutableObjectSetOf() }
                    .add(pluginDependenciesIds.joinToString("|"))
            }
        }

        if (missingDependencies.isNotEmpty()) {
            logMissingDependencies(owner, componentMeta.className, missingDependencies)
            return null
        }

        if (!evaluateConditions(owner, componentMeta, classLoader)) return null

        try {
            val componentClass = Class.forName(componentMeta.className, false, classLoader)
            val componentKClass = componentClass.kotlin
            val objectInstance = componentKClass.objectInstance
            if (objectInstance != null) {
                require(objectInstance is Component) { "Component class must implement Component" }
                return objectInstance
            } else {
                val constructor = componentClass.getConstructor()
                val instance = constructor.newInstance()
                require(instance is Component) { "Component class must implement Component" }
                return instance
            }
        } catch (e: Exception) {
            getLogger(owner).error("Failed to load component ${componentMeta.className}", e)
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun evaluateConditions(
        owner: Any,
        componentMeta: PluginComponentMeta.Component,
        classLoader: ClassLoader
    ): Boolean {
        for (conditionClassName in componentMeta.customConditions) {
            try {
                val conditionClass = Class.forName(conditionClassName, false, classLoader)
                val condition = conditionClass.getConstructor().newInstance() as ComponentCondition
                val logger = getLogger(owner)

                val context = ComponentConditionContext(
                    owner = owner,
                    logger = logger,
                    componentClass = Class.forName(componentMeta.className, false, classLoader) as Class<out Component>
                )

                if (!condition.evaluate(context)) {
                    logger.debug("Component ${componentMeta.className} skipped due to condition $conditionClassName")
                    return false
                }
            } catch (e: Exception) {
                getLogger(owner).error("Failed to evaluate condition $conditionClassName", e)
                return false
            }
        }
        return true
    }

    private fun topologicalSort(
        componentsWithMeta: List<Pair<PluginComponentMeta.Component, Component>>,
        owner: Any
    ): List<Component> {
        // If no components depend on other components, simply sort by priority
        if (componentsWithMeta.none { it.first.componentDependencies.isNotEmpty() }) {
            return componentsWithMeta.map { it.second }.sorted()
        }

        val componentsByClassName = componentsWithMeta.associate { (meta, component) ->
            meta.className to component
        }

        val metaByClassName = componentsWithMeta.associate { (meta, _) ->
            meta.className to meta
        }

        val missingComponentDeps = mutableMapOf<String, MutableSet<String>>()
        for ((meta, _) in componentsWithMeta) {
            for (depClassName in meta.componentDependencies) {
                if (depClassName !in componentsByClassName) {
                    missingComponentDeps.computeIfAbsent(meta.className) { mutableSetOf() }
                        .add(depClassName)
                }
            }
        }

        if (missingComponentDeps.isNotEmpty()) {
            val logger = getLogger(owner)
            for ((componentClassName, missingDeps) in missingComponentDeps) {
                logger.warn(
                    "Component $componentClassName depends on components that are not loaded: ${missingDeps.joinToString(", ")}"
                )
            }
        }

        val validComponents = componentsWithMeta.filter { (meta, _) ->
            meta.className !in missingComponentDeps
        }

        if (validComponents.isEmpty()) {
            return emptyList()
        }

        // Build dependency graph: className -> list of dependents (successors)
        val graph = mutableObject2ObjectMapOf<String, MutableList<String>>()
        val dependencyMap = mutableObject2ObjectMapOf<String, MutableList<String>>()
        
        for ((meta, _) in validComponents) {
            if (meta.className !in graph) {
                graph[meta.className] = mutableListOf()
            }
            if (meta.className !in dependencyMap) {
                dependencyMap[meta.className] = mutableListOf()
            }
            for (depClassName in meta.componentDependencies) {
                if (depClassName in componentsByClassName) {
                    graph.computeIfAbsent(depClassName) { mutableListOf() }.add(meta.className)
                    dependencyMap.computeIfAbsent(meta.className) { mutableListOf() }.add(depClassName)
                }
            }
        }

        // Check for cycles using Tarjan's SCC algorithm
        val cycles = findCyclesWithTarjan(graph, dependencyMap)
        if (cycles.isNotEmpty()) {
            val errorMessage = buildCycleErrorMessage(cycles, metaByClassName)
            throw IllegalStateException(errorMessage)
        }

        // Kahn's algorithm with priority queue for tie-breaking
        val incomingEdges = mutableObject2IntMapOf<String>()
        for ((vertex, successors) in graph) {
            if (vertex !in incomingEdges) {
                incomingEdges[vertex] = 0
            }
            for (successor in successors) {
                incomingEdges.mergeInt(successor, 1, Int::plus)
            }
        }

        // Use a priority queue ordered by component priority (lower priority value = higher priority)
        val queue = PriorityQueue<String>(compareBy { className ->
            componentsByClassName[className]?.priority ?: Short.MAX_VALUE
        })

        incomingEdges.object2IntEntrySet().fastForEach { entry ->
            val vertex = entry.key
            val edges = entry.intValue
            if (edges == 0) queue += vertex
        }

        val result = mutableObjectListOf<Component>()

        while (queue.isNotEmpty()) {
            val vertex = queue.poll()
            componentsByClassName[vertex]?.let { result += it }

            for (successor in graph[vertex].orEmpty()) {
                incomingEdges.mergeInt(successor, -1, Int::minus)
                if (incomingEdges.getInt(successor) == 0) {
                    queue += successor
                }
            }
        }

        return result
    }

    /**
     * Finds all cycles in the dependency graph using Tarjan's Strongly Connected Components algorithm.
     * 
     * @param graph The forward dependency graph (dependency -> dependents)
     * @param dependencyMap The reverse dependency map (component -> its dependencies)
     * @return A list of cycles, where each cycle is a list of class names forming the cycle
     */
    private fun findCyclesWithTarjan(
        graph: Map<String, List<String>>,
        dependencyMap: Map<String, List<String>>
    ): List<List<String>> {
        val nodes = graph.keys.toSet()
        if (nodes.isEmpty()) return emptyList()

        var index = 0
        val nodeIndex = mutableMapOf<String, Int>()
        val lowLink = mutableMapOf<String, Int>()
        val onStack = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        val sccs = mutableListOf<List<String>>()

        fun strongConnect(node: String) {
            nodeIndex[node] = index
            lowLink[node] = index
            index++
            stack.addFirst(node)
            onStack.add(node)

            // Consider all successors (nodes that this node depends on)
            for (dependency in dependencyMap[node].orEmpty()) {
                if (dependency !in nodes) continue
                
                if (dependency !in nodeIndex) {
                    strongConnect(dependency)
                    lowLink[node] = minOf(lowLink[node]!!, lowLink[dependency]!!)
                } else if (dependency in onStack) {
                    lowLink[node] = minOf(lowLink[node]!!, nodeIndex[dependency]!!)
                }
            }

            // If node is a root node, pop the stack and generate an SCC
            if (lowLink[node] == nodeIndex[node]) {
                val scc = mutableListOf<String>()
                do {
                    val w = stack.removeFirst()
                    onStack.remove(w)
                    scc.add(w)
                } while (w != node)
                sccs.add(scc)
            }
        }

        for (node in nodes) {
            if (node !in nodeIndex) {
                strongConnect(node)
            }
        }

        // Filter for SCCs that represent cycles (size > 1 or self-loop)
        val cycles = mutableListOf<List<String>>()
        for (scc in sccs) {
            if (scc.size > 1) {
                // Reconstruct the cycle path
                val cyclePath = reconstructCyclePath(scc, dependencyMap)
                cycles.add(cyclePath)
            } else if (scc.size == 1) {
                // Check for self-loop
                val node = scc[0]
                if (dependencyMap[node]?.contains(node) == true) {
                    cycles.add(listOf(node, node))
                }
            }
        }

        return cycles
    }

    /**
     * Reconstructs a readable cycle path from an SCC.
     */
    private fun reconstructCyclePath(
        scc: List<String>,
        dependencyMap: Map<String, List<String>>
    ): List<String> {
        if (scc.size <= 1) return scc

        val sccSet = scc.toSet()
        val start = scc[0]
        val visited = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun dfs(node: String): Boolean {
            if (node in visited && node == start && path.isNotEmpty()) {
                path.add(node) // Complete the cycle
                return true
            }
            if (node in visited) return false
            
            visited.add(node)
            path.add(node)

            for (dep in dependencyMap[node].orEmpty()) {
                if (dep in sccSet) {
                    if (dfs(dep)) return true
                }
            }

            path.removeAt(path.lastIndex)
            visited.remove(node)
            return false
        }

        dfs(start)
        return if (path.isNotEmpty()) path else scc + scc[0]
    }

    /**
     * Builds a detailed error message for detected cycles.
     */
    private fun buildCycleErrorMessage(
        cycles: List<List<String>>,
        metaByClassName: Map<String, PluginComponentMeta.Component>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Circular component dependencies detected:")
        sb.appendLine()

        cycles.forEachIndexed { index, cycle ->
            val cycleDisplay = cycle.map { it.substringAfterLast('.') }
            sb.appendLine("  Cycle ${index + 1}: ${cycleDisplay.joinToString(" → ")}")
        }

        sb.appendLine()
        sb.appendLine("Details:")

        for (cycle in cycles) {
            for (i in 0 until cycle.size - 1) {
                val from = cycle[i]
                val to = cycle[i + 1]
                val shortFrom = from.substringAfterLast('.')
                val shortTo = to.substringAfterLast('.')
                sb.appendLine("  - $shortFrom depends on $shortTo (via @DependsOnComponent)")
            }
        }

        return sb.toString()
    }

    private fun logMissingDependencies(owner: Any, componentClassName: String, missing: Map<String, Set<String>>) {
        val logger = getLogger(owner)

        val lines = missing.entries
            .sortedBy { it.key }
            .joinToString(separator = System.lineSeparator()) { (type, ids) ->
                val formattedIds = ids.toList().sorted().joinToString(", ")
                "  - $type: $formattedIds"
            }

        logger.warn(
            "Skipping component $componentClassName due to missing dependencies:\n$lines"
        )
    }

    suspend fun getComponents(owner: Any): List<Component> {
        return componentsCache.get(owner)
    }

    fun getComponentsLoaded(owner: Any): List<Component> {
        return componentsCache.underlying().asMap()[owner]?.getNow(emptyList()) ?: emptyList()
    }

    suspend fun getAllComponents(): List<Component> {
        return componentsCache.asMap().values.flatten().sorted()
    }

    fun getAllComponentsLoaded(): List<Component> {
        return componentsCache.underlying().asMap().values.flatMap { it.getNow(emptyList()) }.sorted()
    }

    abstract fun readComponentsFileFromResources(owner: Any, fileName: String): InputStream?
    abstract fun getClassloader(owner: Any): ClassLoader
    abstract fun isPluginLoaded(pluginId: String): Boolean
    abstract fun getLogger(owner: Any): ComponentLogger

    companion object {
        val instance = requiredService<ComponentService>()
        fun get() = instance
    }
}
