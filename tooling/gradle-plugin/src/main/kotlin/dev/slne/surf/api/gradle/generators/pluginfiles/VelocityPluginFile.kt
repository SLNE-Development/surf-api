package dev.slne.surf.api.gradle.generators.pluginfiles

import dev.slne.surf.api.gradle.platform.invalidPluginFile
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.domainObjectContainer
import org.intellij.lang.annotations.Pattern
import org.intellij.lang.annotations.RegExp
import javax.inject.Inject

@RegExp
private const val ID_REGEX = "[a-z][a-z0-9-_]{0,63}"
private val idRegex = ID_REGEX.toRegex()

abstract class VelocityPluginFile @Inject constructor(
    objects: ObjectFactory
) : CommonPluginFile() {
    @get:Pattern(ID_REGEX)
    @get:Input
    abstract val id: Property<String>

    @get:Input
    abstract val main: Property<String>

    @get:Input
    abstract val name: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val description: Property<String>

    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val authors: ListProperty<String>

    @get:Nested
    val pluginDependencies: NamedDomainObjectContainer<Dependency> =
        objects.domainObjectContainer(Dependency::class)

    abstract class Dependency @Inject constructor(
        @get:Input val name: String
    ) {
        @get:Input
        abstract val optional: Property<Boolean>

        @get:Input
        internal abstract val enabled: Property<Boolean>

        init {
            optional.convention(false)
            enabled.convention(true)
        }
    }

    override fun isApplied(): Boolean {
        return main.isPresent
    }

    override fun validate() {
        val id = id.orNull ?: invalidPluginFile("Plugin id not set")
        if (!(idRegex.matches(id))) invalidPluginFile("Invalid plugin id! Should match $idRegex")

        if (version.orNull.isNullOrBlank()) invalidPluginFile("Plugin version not set")
        if (main.orNull.isNullOrBlank()) invalidPluginFile("Main class not set")

        for (dependency in pluginDependencies) {
            if (dependency.name.isBlank()) invalidPluginFile("Dependency id not set")
        }
    }
}