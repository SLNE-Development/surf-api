package dev.slne.surf.api.gradle.generators.pluginfiles

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property

sealed class CommonPluginFile(objects: ObjectFactory) {

    /**
     * The generated main class, used when the build script names none. Absent when the build
     * generates no class.
     */
    internal val generatedMain: Property<String> = objects.property()

    /**
     * The main class the build script names.
     */
    internal abstract fun configuredMain(): Provider<String>

    /**
     * The main class the plugin file refers to: the configured one, else the generated one.
     */
    internal fun effectiveMain(): Provider<String> = configuredMain().orElse(generatedMain)

    internal open fun validate() {
    }
}
