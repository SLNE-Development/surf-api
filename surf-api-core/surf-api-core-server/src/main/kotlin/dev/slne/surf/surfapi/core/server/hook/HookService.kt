package dev.slne.surf.surfapi.core.server.hook

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asLoadingCache
import dev.slne.surf.surfapi.core.api.util.*
import dev.slne.surf.surfapi.shared.api.hook.Hook
import dev.slne.surf.surfapi.shared.api.hook.condition.HookCondition
import dev.slne.surf.surfapi.shared.api.hook.condition.HookConditionContext
import dev.slne.surf.surfapi.shared.internal.hook.HooksConfig
import dev.slne.surf.surfapi.shared.internal.hook.PluginHookMeta
import kotlinx.serialization.SerializationException
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.io.InputStream
import java.util.*

abstract class HookService {

    private val hookMetaCache = Caffeine.newBuilder()
        .weakKeys()
        .build<Any, PluginHookMeta> { owner -> loadHooksMeta(owner) }

    private val hooksCache = Caffeine.newBuilder()
        .weakKeys()
        .asLoadingCache { owner -> loadHooks(owner) }

    private fun loadHooksMeta(owner: Any): PluginHookMeta {
        val rawStream = readHooksFileFromResources(owner, HooksConfig.HOOKS_FILE_NAME) ?: return PluginHookMeta.empty()
        val raw = rawStream.bufferedReader().use { it.readText() }
        return try {
            HooksConfig.json.decodeFromString<PluginHookMeta>(raw)
        } catch (e: SerializationException) {
            getLogger(owner).error("Failed to parse ${HooksConfig.HOOKS_FILE_NAME}", e)
            PluginHookMeta.empty()
        }
    }

    private suspend fun loadHooks(owner: Any): List<Hook> {
        val meta = hookMetaCache.get(owner)
        val classLoader = getClassloader(owner)

        val hooksWithMeta = meta.hooks.mapNotNull { hookMeta ->
            val hook = instantiateHookIfValid(owner, hookMeta, classLoader)
            if (hook != null) {
                hookMeta to hook
            } else {
                null
            }
        }

        return topologicalSort(hooksWithMeta, owner)
    }

    private suspend fun instantiateHookIfValid(
        owner: Any,
        hookMeta: PluginHookMeta.Hook,
        classLoader: ClassLoader
    ): Hook? {
        val missingDependencies = mutableObject2ObjectMapOf<String, MutableSet<String>>()
        for (classDependency in hookMeta.classDependencies) {
            try {
                Class.forName(classDependency, false, classLoader)
            } catch (_: ClassNotFoundException) {
                missingDependencies.computeIfAbsent("Class") { mutableObjectSetOf() }.add(classDependency)
            }
        }

        for (pluginDependencyId in hookMeta.pluginDependencies) {
            if (!isPluginLoaded(pluginDependencyId)) {
                missingDependencies.computeIfAbsent("Plugin") { mutableObjectSetOf() }.add(pluginDependencyId)
            }
        }

        for (pluginDependenciesIds in hookMeta.pluginOneDependencies) {
            if (pluginDependenciesIds.none { isPluginLoaded(it) }) {
                missingDependencies.computeIfAbsent("Plugin (one of)") { mutableObjectSetOf() }
                    .add(pluginDependenciesIds.joinToString("|"))
            }
        }

        if (missingDependencies.isNotEmpty()) {
            logMissingDependencies(owner, hookMeta.className, missingDependencies)
            return null
        }

        if (!evaluateConditions(owner, hookMeta, classLoader)) return null

        try {
            val hookClass = Class.forName(hookMeta.className, false, classLoader)
            val hookKClass = hookClass.kotlin
            val objectInstance = hookKClass.objectInstance
            if (objectInstance != null) {
                require(objectInstance is Hook) { "Hook class must implement Hook" }
                return objectInstance
            } else {
                val constructor = hookClass.getConstructor()
                val instance = constructor.newInstance()
                require(instance is Hook) { "Hook class must implement Hook" }
                return instance
            }
        } catch (e: Exception) {
            getLogger(owner).error("Failed to load hook ${hookMeta.className}", e)
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun evaluateConditions(
        owner: Any,
        hookMeta: PluginHookMeta.Hook,
        classLoader: ClassLoader
    ): Boolean {
        for (conditionClassName in hookMeta.customConditions) {
            try {
                val conditionClass = Class.forName(conditionClassName, false, classLoader)
                val condition = conditionClass.getConstructor().newInstance() as HookCondition
                val logger = getLogger(owner)

                val context = HookConditionContext(
                    owner = owner,
                    logger = logger,
                    hookClass = Class.forName(hookMeta.className, false, classLoader) as Class<out Hook>
                )

                if (!condition.evaluate(context)) {
                    logger.debug("Hook ${hookMeta.className} skipped due to condition $conditionClassName")
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
        hooksWithMeta: List<Pair<PluginHookMeta.Hook, Hook>>,
        owner: Any
    ): List<Hook> {
        // If no hooks depend on other hooks, simply sort by priority
        if (hooksWithMeta.none { it.first.hookDependencies.isNotEmpty() }) {
            return hooksWithMeta.map { it.second }.sorted()
        }

        val hooksByClassName = hooksWithMeta.associate { (meta, hook) ->
            meta.className to hook
        }

        val metaByClassName = hooksWithMeta.associate { (meta, _) ->
            meta.className to meta
        }

        val missingHookDeps = mutableMapOf<String, MutableSet<String>>()
        for ((meta, _) in hooksWithMeta) {
            for (depClassName in meta.hookDependencies) {
                if (depClassName !in hooksByClassName) {
                    missingHookDeps.computeIfAbsent(meta.className) { mutableSetOf() }
                        .add(depClassName)
                }
            }
        }

        if (missingHookDeps.isNotEmpty()) {
            val logger = getLogger(owner)
            for ((hookClassName, missingDeps) in missingHookDeps) {
                logger.warn(
                    "Hook $hookClassName depends on hooks that are not loaded: ${missingDeps.joinToString(", ")}"
                )
            }
        }

        val validHooks = hooksWithMeta.filter { (meta, _) ->
            meta.className !in missingHookDeps
        }

        if (validHooks.isEmpty()) {
            return emptyList()
        }

        // Build dependency graph: className -> list of dependents (successors)
        // Note: In Kahn's algorithm, edges go from dependency to dependent
        val graph = mutableObject2ObjectMapOf<String, MutableList<String>>()
        for ((meta, _) in validHooks) {
            // Ensure all nodes exist in the graph
            if (meta.className !in graph) {
                graph[meta.className] = mutableListOf()
            }
            // Add edges from dependencies to this hook
            for (depClassName in meta.hookDependencies) {
                if (depClassName in hooksByClassName) {
                    graph.computeIfAbsent(depClassName) { mutableListOf() }.add(meta.className)
                }
            }
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

        // Use a priority queue ordered by hook priority (lower priority value = higher priority)
        val queue = PriorityQueue<String>(compareBy { className ->
            hooksByClassName[className]?.priority ?: Short.MAX_VALUE
        })
        
        incomingEdges.object2IntEntrySet().fastForEach { entry ->
            val vertex = entry.key
            val edges = entry.intValue
            if (edges == 0) queue += vertex
        }

        val result = mutableObjectListOf<Hook>()

        while (queue.isNotEmpty()) {
            val vertex = queue.poll()
            hooksByClassName[vertex]?.let { result += it }

            for (successor in graph[vertex].orEmpty()) {
                incomingEdges.mergeInt(successor, -1, Int::minus)
                if (incomingEdges.getInt(successor) == 0) {
                    queue += successor
                }
            }
        }

        if (result.size != incomingEdges.size) {
            val chain = findCyclicDependency(graph, incomingEdges)
            throw IllegalStateException(
                "Circular hook dependency detected: ${chain.joinToString(" -> ")}"
            )
        }

        return result
    }

    private fun findCyclicDependency(
        graph: Map<String, List<String>>,
        incomingEdges: Map<String, Int>
    ): List<String> {
        // Find nodes that are part of a cycle (still have incoming edges after topological sort)
        val nodesInCycle = incomingEdges.filter { it.value > 0 }.keys
        if (nodesInCycle.isEmpty()) return emptyList()

        // Use DFS to find an actual cycle path
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun dfs(node: String): List<String>? {
            if (node in recursionStack) {
                // Found a cycle! Build the cycle path
                val cycleStart = path.indexOf(node)
                return if (cycleStart >= 0) {
                    path.subList(cycleStart, path.size) + node
                } else {
                    listOf(node)
                }
            }

            if (node in visited) return null

            visited.add(node)
            recursionStack.add(node)
            path.add(node)

            // Explore successors
            for (successor in graph[node].orEmpty()) {
                val cycle = dfs(successor)
                if (cycle != null) return cycle
            }

            recursionStack.remove(node)
            path.removeAt(path.lastIndex)
            return null
        }

        // Start DFS from any node that's part of a cycle
        for (startNode in nodesInCycle) {
            if (startNode !in visited) {
                val cycle = dfs(startNode)
                if (cycle != null) return cycle
            }
        }

        // Fallback: if no cycle found via DFS, return the nodes with remaining edges
        return nodesInCycle.toList()
    }


    private fun logMissingDependencies(owner: Any, hookClassName: String, missing: Map<String, Set<String>>) {
        val logger = getLogger(owner)

        val lines = missing.entries
            .sortedBy { it.key }
            .joinToString(separator = System.lineSeparator()) { (type, ids) ->
                val formattedIds = ids.toList().sorted().joinToString(", ")
                "  - $type: $formattedIds"
            }

        logger.warn(
            "Skipping hook $hookClassName due to missing dependencies:\n$lines"
        )
    }

    suspend fun getHooks(owner: Any): List<Hook> {
        return hooksCache.get(owner)
    }

    fun getHooksLoaded(owner: Any): List<Hook> {
        return hooksCache.underlying().asMap()[owner]?.getNow(emptyList()) ?: emptyList()
    }

    suspend fun getAllHooks(): List<Hook> {
        return hooksCache.asMap().values.flatten().sorted()
    }

    fun getAllHooksLoaded(): List<Hook> {
        return hooksCache.underlying().asMap().values.flatMap { it.getNow(emptyList()) }.sorted()
    }

    abstract fun readHooksFileFromResources(owner: Any, fileName: String): InputStream?
    abstract fun getClassloader(owner: Any): ClassLoader
    abstract fun isPluginLoaded(pluginId: String): Boolean
    abstract fun getLogger(owner: Any): ComponentLogger

    companion object {
        val instance = requiredService<HookService>()
        fun get() = instance
    }
}