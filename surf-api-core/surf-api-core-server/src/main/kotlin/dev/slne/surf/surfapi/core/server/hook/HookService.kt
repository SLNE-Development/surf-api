package dev.slne.surf.surfapi.core.server.hook

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asLoadingCache
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import dev.slne.surf.surfapi.core.api.util.requiredService
import dev.slne.surf.surfapi.shared.api.hook.Hook
import dev.slne.surf.surfapi.shared.api.hook.condition.HookCondition
import dev.slne.surf.surfapi.shared.api.hook.condition.HookConditionContext
import dev.slne.surf.surfapi.shared.internal.hook.HooksConfig
import dev.slne.surf.surfapi.shared.internal.hook.PluginHookMeta
import kotlinx.serialization.SerializationException
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.io.InputStream

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

        val sorted = mutableListOf<Hook>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        fun visit(className: String) {
            if (className in visited) return

            if (className in visiting) {
                val chain = buildCircularDependencyChain(className, metaByClassName, visiting)
                throw IllegalStateException(
                    "Circular hook dependency detected: ${chain.joinToString(" -> ")}"
                )
            }

            visiting.add(className)

            val dependencies = metaByClassName[className]?.hookDependencies ?: emptyList()
            // Sort dependencies by priority to use priority as a tie-breaker
            val sortedDeps = dependencies.sortedBy { depClassName ->
                hooksByClassName[depClassName]?.priority ?: Short.MAX_VALUE
            }
            for (depClassName in sortedDeps) {
                if (depClassName in hooksByClassName) {
                    visit(depClassName)
                }
            }

            visiting.remove(className)
            visited.add(className)

            hooksByClassName[className]?.let { sorted.add(it) }
        }

        // Sort validHooks by priority before visiting to use priority as a tie-breaker
        validHooks.sortedBy { (_, hook) -> hook.priority }.forEach { (meta, _) -> visit(meta.className) }
        return sorted
    }

    private fun buildCircularDependencyChain(
        startClassName: String,
        metaByClassName: Map<String, PluginHookMeta.Hook>,
        visiting: Set<String>
    ): List<String> {
        val chain = mutableListOf<String>()
        var current = startClassName

        while (current !in chain) {
            chain.add(current)
            val deps = metaByClassName[current]?.hookDependencies ?: break
            current = deps.firstOrNull { it in visiting } ?: break
        }

        chain.add(startClassName)
        return chain
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