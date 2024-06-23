package dev.slne.surf.surfapi

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.surfapi.dependencies.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

const val API_SCOPE = "api"
const val COMPILE_ONLY_API_SCOPE = "compileOnly"

abstract class SurfApiPluginExtension @Inject constructor(objects: ObjectFactory) {
    @Internal
    @Input
    val internal: Property<Boolean> = objects.property<Boolean>().convention(false)

    @Input
    val version: Property<String> = objects.property<String>().convention("1.20.4-1.0.0-SNAPSHOT")

    @Input
    var pluginType: Property<PluginType> =
        objects.property<PluginType>().convention(PluginType.CORE_STANDALONE)

    @Input
    var relocationBasePackage: Property<String> =
        objects.property<String>().convention("dev.slne.surf")
}

enum class PluginType(internal val shade: Boolean) {
    CORE_STANDALONE(true),
    CORE_API(false),
    BUKKIT_STANDALONE(true),
    BUKKIT_API(false),
    VELOCITY_STANDALONE(true),
    VELOCITY_API(false);

    internal fun scope() = if (shade) API_SCOPE else COMPILE_ONLY_API_SCOPE
    internal fun hasBukkit() = this == BUKKIT_STANDALONE || this == BUKKIT_API
    internal fun hasVelocity() = this == VELOCITY_STANDALONE || this == VELOCITY_API
}

abstract class SurfApiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("surfApi", SurfApiPluginExtension::class.java)
        target.afterEvaluate {
            val extension = target.extensions.getByType(SurfApiPluginExtension::class.java)

            extension.apply {
                val type: PluginType by pluginType
                val internal: Boolean by internal
                val version: String by version

                val scope = if (internal) COMPILE_ONLY_API_SCOPE else type.scope()
                val dependencies = mutableMapOf<String, MutableList<Dependency>>()

                fun DependencyHandler.apply(
                    scope: String,
                    dependency: Dependency,
                    requireNonInternal: Boolean = false
                ) {
                    if (internal && requireNonInternal) {
                        return
                    }

                    add(scope, dependency.notation)
                    dependencies.computeIfAbsent(scope) { mutableListOf() }.add(dependency)
                }

                target.dependencies.apply {
                    // Adventure Libraries
                    apply(COMPILE_ONLY_API_SCOPE, ADVENTURE_API)
                    apply(COMPILE_ONLY_API_SCOPE, ADVENTURE_TEXT_LOGGER_SLF4J)
                    apply(COMPILE_ONLY_API_SCOPE, ADVENTURE_TEXT_MINIMESSAGE)
                    apply(COMPILE_ONLY_API_SCOPE, ADVENTURE_TEXT_SERIALIZER_GSON)
                    apply(COMPILE_ONLY_API_SCOPE, ADVENTURE_TEXT_SERIALIZER_LEGACY)
                    apply(COMPILE_ONLY_API_SCOPE, ADVENTURE_TEXT_SERIALIZER_PLAIN)
                    apply(COMPILE_ONLY_API_SCOPE, ADVENTURE_TEXT_SERIALIZER_ANSI)

                    // Core dependencies
                    apply(scope, SPONGEPOWERED_MATH)
                    apply(scope, OKHTTP)
                    apply(scope, CONFIGURATE_YAML)
                    apply(scope, CONFIGURATE_JACKSON)
                    apply(scope, GUAVA)
                    apply(COMPILE_ONLY_API_SCOPE, BRIGADIER)
                    apply(scope, DAZZLECONF)
                    apply(scope, COMMONS_LANG3)
                    apply(scope, COMMONS_TEXT)
                    apply(scope, CAFFEINE)
                    apply(scope, GSON)
                    if (type == PluginType.CORE_STANDALONE) {
                        apply(scope, CORE_STANDALONE.resolve(version), true)
                    } else if (!type.shade) {
                        apply(COMPILE_ONLY_API_SCOPE, PACKET_EVENTS_API)
                        apply(COMPILE_ONLY_API_SCOPE, FASTUTIL)
                        apply(COMPILE_ONLY_API_SCOPE, COMMAND_API_CORE)
                        apply(API_SCOPE, KOTLIN_COROUTINES)
                    }

                    if (type == PluginType.CORE_API) {
                        apply(COMPILE_ONLY_API_SCOPE, CORE_API.resolve(version), true)
                    }

                    if (type.hasBukkit()) {
                        if (type == PluginType.BUKKIT_STANDALONE) { // TODO: 23.06.2024 12:45 - bukkit standalone?
                            apply(COMPILE_ONLY_API_SCOPE, CORE_API.resolve(version), true)
                        } else {
                            apply(COMPILE_ONLY_API_SCOPE, BUKKIT_API.resolve(version), true)
                            apply(COMPILE_ONLY_API_SCOPE, PACKET_EVENTS_SPIGOT)
                            apply(COMPILE_ONLY_API_SCOPE, SCOREBOARD_LIBRARY_API)
                            apply(COMPILE_ONLY_API_SCOPE, COMMAND_API_BUKKIT)
                            apply(API_SCOPE, COMMAND_API_BUKKIT_KOTLIN)
                            apply(COMPILE_ONLY_API_SCOPE, REFLECTION_REMAPPER)
                            apply(COMPILE_ONLY_API_SCOPE, MORE_PERSISTENT_DATA_TYPES)
                            apply(API_SCOPE, INVENTORY_FRAMEWORK)
                        }
                    }

                    if (type.hasVelocity()) {
                        if (type == PluginType.VELOCITY_STANDALONE) { // TODO: 23.06.2024 12:59 - velocity standalone?
                            apply(COMPILE_ONLY_API_SCOPE, CORE_API.resolve(version), true)
                        } else {
                            apply(COMPILE_ONLY_API_SCOPE, VELOCITY_API.resolve(version), true)
                            apply(COMPILE_ONLY_API_SCOPE, PACKET_EVENTS_VELOCITY)
                            apply(COMPILE_ONLY_API_SCOPE, COMMAND_API_VELOCITY)
                        }
                    }
                }

                if (type.shade) {
                    if (!target.plugins.hasPlugin("com.github.johnrengelman.shadow")) {
                        throw IllegalStateException("Standalone plugins require the shadow plugin to be applied")
                    }

                    val relocationBasePackage: String by relocationBasePackage
                    target.tasks.withType(ShadowJar::class).configureEach {
                        dependencies.forEach { (scope, deps) ->
                            if (scope == API_SCOPE) {
                                deps.forEach {
                                    it.relocationPattern.forEach { pattern ->
                                        relocate(
                                            pattern,
                                            relocationBasePackage + "." + it.relocationPackage
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}