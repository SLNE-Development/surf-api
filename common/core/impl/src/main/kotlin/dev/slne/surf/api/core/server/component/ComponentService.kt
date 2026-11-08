package dev.slne.surf.api.core.server.component

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asLoadingCache
import dev.slne.surf.api.core.server.component.environment.EnvironmentConfig
import dev.slne.surf.api.core.server.component.property.PropertyService
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.core.util.mutableObjectSetOf
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.component.SurfComponent
import dev.slne.surf.api.shared.api.component.condition.ComponentCondition
import dev.slne.surf.api.shared.api.component.condition.ComponentConditionContext
import dev.slne.surf.api.shared.api.component.processor.ComponentContext
import dev.slne.surf.api.shared.api.component.processor.ComponentPostProcessor
import dev.slne.surf.api.shared.api.component.requirement.ConditionalOnEnvironment
import dev.slne.surf.api.shared.api.component.requirement.ConditionalOnMissingComponent
import dev.slne.surf.api.shared.api.component.requirement.ConditionalOnProperty
import dev.slne.surf.api.shared.internal.hook.ComponentsConfig
import dev.slne.surf.api.shared.internal.hook.PluginComponentMeta
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.io.File
import java.net.URI
import java.nio.file.Path
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
        .build<Any, List<ComponentPostProcessor>> { owner -> loadPostProcessors(owner) }

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
                    JarFile(File(URI(jarPath))).use { jarFile ->
                        jarFile.entries().asSequence()
                            .filter {
                                it.name.startsWith(ComponentsConfig.COMPONENTS_DIRECTORY) && it.name.endsWith(
                                    ".json"
                                )
                            }
                            .forEach { entry ->
                                try {
                                    val raw = jarFile.getInputStream(entry).bufferedReader()
                                        .use { it.readText() }
                                    val decoded =
                                        ComponentsConfig.json.decodeFromString<PluginComponentMeta>(
                                            raw
                                        )
                                    meta += decoded
                                } catch (e: Exception) {
                                    logger.error("Failed to parse ${entry.name}", e)
                                }
                            }
                    }
                } else {
                    val dir = File(url.toURI())
                    dir.listFiles { file -> file.extension == "json" }?.forEach { file ->
                        try {
                            val raw = file.readText()
                            val decoded =
                                ComponentsConfig.json.decodeFromString<PluginComponentMeta>(raw)
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

        return meta.sorted()
    }

    private suspend fun loadComponents(owner: Any): List<ComponentEntry> {
        val meta = componentMetaCache.get(owner)
        val classLoader = getClassloader(owner)

        val sortedComponentMetas = ComponentSorting.topologicalSort(meta.components)
        val loadedComponents = mutableObjectListOf<ComponentEntry>()
        for (meta in sortedComponentMetas) {
            val component = instantiateComponentIfValid(owner, loadedComponents, meta, classLoader)
            if (component != null) {
                loadedComponents.add(component)
            }
        }

        val postProcessors = postProcessorsCache.get(owner)
        return applyPostProcessors(loadedComponents, postProcessors, owner)
    }

    private fun loadPostProcessors(owner: Any): List<ComponentPostProcessor> {
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
        components: List<ComponentEntry>,
        postProcessors: List<ComponentPostProcessor>,
        owner: Any
    ): List<ComponentEntry> {
        if (postProcessors.isEmpty()) {
            return components
        }

        val context = ComponentContext(
            owner = owner,
            allComponents = components.map { it.component }
        )

        return components.map { entry ->
            var processedComponent = entry
            for (processor in postProcessors) {
                processedComponent = ComponentEntry(
                    processor.postProcessAfterInitialization(
                        processedComponent.component,
                        processedComponent.component::class.qualifiedName
                            ?: processedComponent.component::class.java.name,
                        context
                    ), entry.priority
                )
            }
            processedComponent
        }
    }

    suspend fun invokePostProcessorsBeforeDestruction(owner: Any) {
        val components = awaitLoadedComponents(owner)
        val postProcessors = postProcessorsCache.getIfPresent(owner) ?: return

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
        loadedComponents: List<ComponentEntry>,
        componentMeta: PluginComponentMeta.Component,
        classLoader: ClassLoader
    ): ComponentEntry? {
        val logger = getLogger(owner)
        val missingDependencies = mutableObject2ObjectMapOf<String, MutableSet<String>>()
        for (classDependency in componentMeta.classDependencies) {
            try {
                Class.forName(classDependency, false, classLoader)
            } catch (_: ClassNotFoundException) {
                missingDependencies.computeIfAbsent("Class") { mutableObjectSetOf() }
                    .add(classDependency)
            }
        }

        for (pluginDependencyId in componentMeta.pluginDependencies) {
            if (!isPluginLoaded(pluginDependencyId)) {
                missingDependencies.computeIfAbsent("Plugin") { mutableObjectSetOf() }
                    .add(pluginDependencyId)
            }
        }

        for (pluginDependenciesIds in componentMeta.pluginOneDependencies) {
            if (pluginDependenciesIds.none { isPluginLoaded(it) }) {
                missingDependencies.computeIfAbsent("Plugin (one of)") { mutableObjectSetOf() }
                    .add(pluginDependenciesIds.joinToString("|"))
            }
        }

        for (componentDependency in componentMeta.componentDependencies) {
            val isLoaded = loadedComponents.any { (component) ->
                val kClass = component::class
                val className = kClass.qualifiedName ?: kClass.java.name
                className == componentDependency
            }
            if (!isLoaded) {
                missingDependencies.computeIfAbsent("Component") { mutableObjectSetOf() }
                    .add(componentDependency)
            }
        }

        if (missingDependencies.isNotEmpty()) {
            logMissingDependencies(owner, componentMeta.className, missingDependencies)
            return null
        }

        // Evaluate @ConditionalOnEnvironment
        if (!evaluateEnvironmentConditions(componentMeta, logger)) return null

        // Evaluate @ConditionalOnMissingComponent
        if (!evaluateMissingComponentConditions(
                componentMeta,
                loadedComponents,
                classLoader,
                logger
            )
        ) return null

        // Evaluate @ConditionalOnProperty
        if (!evaluatePropertyConditions(owner, componentMeta, logger)) return null

        if (!evaluateConditions(owner, componentMeta, classLoader)) return null

        try {
            val hookClass = Class.forName(componentMeta.className, false, classLoader)
            val hookKClass = hookClass.kotlin
            val objectInstance = hookKClass.objectInstance
            if (objectInstance != null) {
                require(objectInstance is SurfComponent) { "Component class must implement Component" }
                return ComponentEntry(objectInstance, componentMeta.priority)
            } else {
                val constructor = hookClass.getConstructor()
                val instance = constructor.newInstance()
                require(instance is SurfComponent) { "Component class must implement Component" }
                return ComponentEntry(instance, componentMeta.priority)
            }
        } catch (e: Exception) {
            logger.error("Failed to load component ${componentMeta.className}", e)
        }

        return null
    }

    /**
     * Evaluates @[ConditionalOnEnvironment] conditions.
     * Returns true if all environment conditions are satisfied.
     */
    private fun evaluateEnvironmentConditions(
        componentMeta: PluginComponentMeta.Component,
        logger: ComponentLogger
    ): Boolean {
        for (environments in componentMeta.conditionalOnEnvironments) {
            if (environments.isEmpty()) continue

            if (!EnvironmentConfig.matchesAny(environments)) {
                logger.debug(
                    "Component {} skipped: environments '{}' does not match required environments {}",
                    componentMeta.className,
                    EnvironmentConfig.getEnvironments(),
                    environments
                )
                return false
            }
        }
        return true
    }

    /**
     * Evaluates @[ConditionalOnMissingComponent] conditions.
     * Returns true if all referenced components are NOT loaded.
     */
    private fun evaluateMissingComponentConditions(
        componentMeta: PluginComponentMeta.Component,
        loadedComponents: List<ComponentEntry>,
        classLoader: ClassLoader,
        logger: ComponentLogger
    ): Boolean {
        for (missingComponent in componentMeta.conditionalOnMissingComponents) {
            val isLoaded = loadedComponents.any { (component) ->
                val loadedComponentClass = component.javaClass
                val missingComponentClass = try {
                    Class.forName(missingComponent, false, classLoader)
                } catch (_: ClassNotFoundException) {
                    null
                }

                missingComponentClass != null && missingComponentClass.isAssignableFrom(
                    loadedComponentClass
                )
            }
            if (isLoaded) {
                logger.debug(
                    "Component {} skipped: component {} is present (ConditionalOnMissingComponent)",
                    componentMeta.className,
                    missingComponent
                )
                return false
            }
        }
        return true
    }

    /**
     * Evaluates @[ConditionalOnProperty] conditions.
     * Returns true if all property conditions are satisfied.
     */
    private fun evaluatePropertyConditions(
        owner: Any,
        componentMeta: PluginComponentMeta.Component,
        logger: ComponentLogger
    ): Boolean {
        val dataPath = getDataPath(owner)

        for (propertyCondition in componentMeta.conditionalOnProperties) {
            val value =
                PropertyService.getProperty(dataPath, propertyCondition.key, propertyCondition.file)
            val exists = value != null

            val matches = when {
                !exists -> propertyCondition.matchIfMissing
                propertyCondition.havingValue.isEmpty() -> true
                else -> value.equals(propertyCondition.havingValue, ignoreCase = true)
            }

            if (!matches) {
                logger.debug(
                    "Component {} skipped: property '{}' condition not satisfied (expected='{}', actual='{}', matchIfMissing={})",
                    componentMeta.className,
                    propertyCondition.key.joinToString("."),
                    propertyCondition.havingValue,
                    value ?: "<missing>",
                    propertyCondition.matchIfMissing
                )
                return false
            }
        }
        return true
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
                    componentClass = Class.forName(
                        componentMeta.className,
                        false,
                        classLoader
                    ) as Class<out SurfComponent>
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

    private fun logMissingDependencies(
        owner: Any,
        componentClassName: String,
        missing: Map<String, Set<String>>
    ) {
        val logger = getLogger(owner)

        val lines = missing.entries
            .sortedBy { it.key }
            .joinToString(separator = System.lineSeparator()) { (type, ids) ->
                val formattedIds = ids.toList().sorted().joinToString(", ")
                "  — $type: $formattedIds"
            }

        logger.warn(
            "Skipping component $componentClassName due to missing dependencies:\n$lines"
        )
    }

    suspend fun getOrLoadComponents(owner: Any): List<SurfComponent> {
        return componentsCache.get(owner).map { it.component }
    }

    fun getLoadedComponents(owner: Any): List<SurfComponent> {
        return (componentsCache.underlying().asMap()[owner]?.getNow(emptyList())
            ?: emptyList()).map { it.component }
    }

    suspend fun awaitLoadedComponents(owner: Any): List<SurfComponent> {
        return componentsCache.getIfPresent(owner)?.map { it.component } ?: emptyList()
    }

    suspend fun getAllComponents(): List<SurfComponent> {
        return componentsCache.asMap().values.flatten().map { it.component }
    }

    fun getAllComponentsLoaded(): List<SurfComponent> {
        return componentsCache.underlying().asMap().values.flatMap { it.getNow(emptyList()) }
            .map { it.component }
    }

    abstract fun getClassloader(owner: Any): ClassLoader
    abstract fun isPluginLoaded(pluginId: String): Boolean
    abstract fun getLogger(owner: Any): ComponentLogger
    abstract fun getDataPath(owner: Any): Path

    companion object {
        val instance = requiredService<ComponentService>()
        fun get() = instance
    }

    data class ComponentEntry(
        val component: SurfComponent,
        val priority: Short,
    ) : Comparable<ComponentEntry> {
        override fun compareTo(other: ComponentEntry): Int {
            return this.priority.compareTo(other.priority)
        }
    }
}