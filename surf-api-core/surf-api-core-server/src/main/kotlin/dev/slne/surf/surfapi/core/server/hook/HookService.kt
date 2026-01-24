package dev.slne.surf.surfapi.core.server.hook

import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.surfapi.core.api.hook.AbstractHook
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import dev.slne.surf.surfapi.core.api.util.requiredService
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.io.InputStream

abstract class HookService {

    private val hookMetaCache = Caffeine.newBuilder()
        .weakKeys()
        .build<Any, PluginHookMeta> { owner -> loadHooksMeta(owner) }

    private val hooksCache = Caffeine.newBuilder()
        .weakKeys()
        .build<Any, List<AbstractHook>> { owner -> loadHooks(owner) }

    private fun loadHooksMeta(owner: Any): PluginHookMeta {
        val rawStream = readHooksFileFromResources(owner, HOOKS_FILE_NAME) ?: return PluginHookMeta.empty()
        val raw = rawStream.bufferedReader().use { it.readText() }
        return try {
            Json.decodeFromString<PluginHookMeta>(raw)
        } catch (e: SerializationException) {
            getLogger(owner).error("Failed to parse $HOOKS_FILE_NAME", e)
            PluginHookMeta.empty()
        }
    }

    private fun loadHooks(owner: Any): List<AbstractHook> {
        val meta = hookMetaCache.get(owner)
        val classLoader = getClassloader(owner)
        val hooks = mutableObjectListOf<AbstractHook>()

        for (hookMeta in meta.hooks) {
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
                continue
            }

            try {
                val hookClass = Class.forName(hookMeta.className, false, classLoader)
                val hookKClass = hookClass.kotlin
                val objectInstance = hookKClass.objectInstance
                if (objectInstance != null) {
                    require(objectInstance is AbstractHook) { "Hook class must implement AbstractHook" }
                    hooks.add(objectInstance)
                } else {
                    val constructor = hookClass.getConstructor()
                    val instance = constructor.newInstance()
                    require(instance is AbstractHook) { "Hook class must implement AbstractHook" }
                    hooks.add(instance)
                }
            } catch (e: Exception) {
                getLogger(owner).error("Failed to load hook ${hookMeta.className}", e)
            }
        }

        return hooks.sorted()
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

    fun getHooks(owner: Any): List<AbstractHook> {
        return hooksCache.get(owner)
    }

    fun getAllHooks(): List<AbstractHook> {
        return hooksCache.asMap().values.flatten().sorted()
    }

    abstract fun readHooksFileFromResources(owner: Any, fileName: String): InputStream?
    abstract fun getClassloader(owner: Any): ClassLoader
    abstract fun isPluginLoaded(pluginId: String): Boolean
    abstract fun getLogger(owner: Any): ComponentLogger

    companion object {
        const val HOOKS_FILE_NAME = "surf-hooks.json"

        val instance = requiredService<HookService>()
        fun get() = instance
    }
}