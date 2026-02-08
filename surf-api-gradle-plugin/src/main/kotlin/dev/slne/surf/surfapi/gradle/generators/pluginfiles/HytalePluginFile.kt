package dev.slne.surf.surfapi.gradle.generators.pluginfiles

import dev.slne.surf.surfapi.gradle.platform.invalidPluginFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class HytalePluginFile @Inject constructor() : CommonPluginFile() {
    @get:Input
    abstract val group: Property<String>

    @get:Input
    abstract val id: Property<String>

    @get:Input
    abstract val name: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val description: Property<String>

    @get:Input
    abstract val authors: ListProperty<String>

    @get:Input
    abstract val website: Property<String>

    @get:Input
    abstract val serverVersion: Property<String>

    @get:Input
    abstract val disabledByDefault: Property<Boolean>

    @get:Input
    abstract val main: Property<String>

    @get:Input
    abstract val includesAssetPack: Property<Boolean>

    @get:Input
    abstract val dependencies: MapProperty<String, String>

    @get:Input
    abstract val optionalDependencies: MapProperty<String, String>

    override fun isApplied(): Boolean {
        return main.isPresent
    }

    override fun validate() {
        if (name.orNull.isNullOrBlank()) invalidPluginFile("Plugin name not set")

        if (version.orNull.isNullOrBlank()) invalidPluginFile("Plugin version not set")
        if (main.orNull.isNullOrBlank()) invalidPluginFile("Main class not set")

        optionalDependencies.orNull?.forEach { (dependency, version) ->
            if (dependency.isBlank()) invalidPluginFile("Dependency id not set")
            if (version.isBlank()) invalidPluginFile("Dependency '$dependency' version not set")
        }
    }
}