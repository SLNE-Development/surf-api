package dev.slne.surf.api.gradle.platform.minestom

import dev.slne.surf.api.gradle.generators.GeneratePluginFile
import dev.slne.surf.api.gradle.generators.pluginfiles.MinestomPluginFile
import dev.slne.surf.api.gradle.generators.pluginfiles.dto.MinestomPluginFileDto
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.CommonSurfPluginWithPluginFile
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingConfigurer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * A plugin for a surf Minestom server, loaded from its `plugins` directory or shaded into it.
 *
 * Generates the plugin's `minestom-plugin.json` from `minestomPluginFile { }`, together with the
 * class path index the server finds shaded plugins through.
 */
internal class MinestomSurfPlugin :
    CommonSurfPluginWithPluginFile<MinestomSurfExtension, MinestomPluginFile, MinestomPluginFileDto>(
        "minestom",
        SurfApiPlatform.MINESTOM,
        PLUGIN_FILE_NAME,
    ) {

    init {
        applyMinestomRelocations()
    }

    override val extensionClass = MinestomSurfExtension::class.java
    override val dtoSerializer = MinestomPluginFileDto.serializer()

    override fun apply(target: Project) {
        super.apply(target)
        target.registerClassPathIndex()
    }

    override fun createPluginFile(project: Project) =
        project.extensions.create<MinestomPluginFile>("minestomPluginFile").apply {
            id.convention(project.name.lowercase())
            name.convention(project.provider { project.name })
            version.convention(project.provider { project.version.toString() })
            description.convention(project.provider { project.description ?: "" })
            url.convention(project.providers.gradleProperty("url"))

            val ext = project.extensions.getByType(MinestomSurfExtension::class.java)
            authors.convention(ext.authors)

            val coreEnabled = ext.coreModule.map { true }.orElse(false)

            pluginDependencies {
                register("surf-core-minestom") {
                    optional.convention(false)
                    enabled.convention(coreEnabled)
                }
            }
        }

    override fun createPluginFileDto(pluginFile: MinestomPluginFile) =
        MinestomPluginFileDto.fromFile(pluginFile)

    /**
     * Writes the files a server reads plugins on its own class path from: a service entry with
     * the main class, which survives when several plugins are shaded into one jar, and a copy of
     * the plugin file named after it.
     */
    private fun Project.registerClassPathIndex() {
        val pluginFile = extensions.getByType(MinestomPluginFile::class.java)
        val outputDirectory = layout.buildDirectory.dir("generated/surf-api/minestom-classpath")

        val applied = provider { pluginFile.isApplied() }
        val mainClass = pluginFile.main.orElse("")

        val generateIndex = tasks.register<GeneratePluginFile>("generateMinestomPluginIndex") {
            group = "surf-api"
            fileName.set(CLASSPATH_INDEX)
            outputDir.set(outputDirectory.map { it.dir("index") })
            pluginFileJson.set(applied.zip(mainClass) { isApplied, main -> if (isApplied) main else "" })
        }

        val generateCopy = tasks.register<GeneratePluginFile>("generateMinestomPluginClassPathFile") {
            group = "surf-api"
            fileName.set(mainClass.map { main -> "$CLASSPATH_DIRECTORY$main.json" })
            outputDir.set(outputDirectory.map { it.dir("descriptor") })
            pluginFileJson.set(provider {
                if (!pluginFile.isApplied()) return@provider ""
                pluginFile.validate()
                GeneratePluginFile.json.encodeToString(
                    MinestomPluginFileDto.serializer(),
                    MinestomPluginFileDto.fromFile(pluginFile),
                )
            })
        }

        plugins.withType<JavaPlugin> {
            extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                resources.srcDir(generateIndex)
                resources.srcDir(generateCopy)
            }
        }
    }

    override fun Project.platformTestDependencies(extension: MinestomSurfExtension) {
        val testing = extension.testing
        if (!testing.minestomTesting.get()) return
        dependencies {
            add(
                SurfTestingConfigurer.TEST_IMPLEMENTATION,
                "net.minestom:testing:${testing.minestomTestingVersion.get()}"
            )
        }
    }

    internal companion object {
        const val PLUGIN_FILE_NAME = "minestom-plugin.json"
        const val CLASSPATH_INDEX = "META-INF/services/dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin"
        const val CLASSPATH_DIRECTORY = "META-INF/surf-minestom-plugins/"
    }
}

/**
 * The relocations every surf Minestom jar applies, plugins and servers alike, so that a plugin's
 * bytecode refers to the same packages as the server it runs in.
 */
internal fun dev.slne.surf.api.gradle.platform.common.CommonSurfPlugin<*>.applyMinestomRelocations() {
    applyRelocation("com.mojang.serialization", "mojang.serialization")
    applyRelocation("com.mojang.datafixers", "mojang.datafixers")
    applyRelocation("net.kyori.adventure.nbt", "kyori.nbt", listOf("net.kyori.adventure.nbt.api.**"))
    applyRelocation("it.unimi.dsi.fastutil", "fastutil")
    applyRelocation("me.devnatan.inventoryframework", "devnatan.inventoryframework")
}
