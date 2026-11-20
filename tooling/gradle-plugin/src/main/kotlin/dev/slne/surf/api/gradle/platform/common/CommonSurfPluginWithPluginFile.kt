package dev.slne.surf.api.gradle.platform.common

import dev.slne.surf.api.gradle.generators.GeneratePluginFile
import dev.slne.surf.api.gradle.generators.pluginfiles.CommonPluginFile
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import dev.slne.surf.api.gradle.pluginclass.GeneratedPluginClass
import dev.slne.surf.api.gradle.pluginclass.SurfPluginClassTasks
import kotlinx.serialization.SerializationStrategy
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.kotlin.dsl.withType

/**
 * Base of the platform plugins that describe the plugin in a file of their own and generate the main
 * class [generatedClass] unless the build script names one in that file.
 */
abstract class CommonSurfPluginWithPluginFile<E : CoreSurfExtension, F : CommonPluginFile, D>(
    platformName: String,
    private val platform: SurfApiPlatform,
    private val pluginFileName: String,
    private val generatedClass: GeneratedPluginClass,
    private val generatedClassDirectory: String,
) : CommonSurfPlugin<E>(platformName, platform) {

    protected abstract val dtoSerializer: SerializationStrategy<D>

    protected abstract fun createPluginFile(project: Project): F
    protected abstract fun createPluginFileDto(pluginFile: F): D

    override fun apply(target: Project) {
        super.apply(target)

        with(target) {
            val generatedResourcesDirectory =
                layout.buildDirectory.dir("generated/surf-api/$platformName")

            val pluginClasses = SurfPluginClassTasks(this, extensions.getByType(extensionClass), platform)
            val createdPluginFile = createPluginFile(this)
            registerPluginClasses(pluginClasses, createdPluginFile)

            val generateTask =
                tasks.register<GeneratePluginFile>("generate${platformName.uppercaseFirstChar()}PluginFile") {
                    group = "surf-api"

                    fileName.set(pluginFileName)
                    outputDir.set(generatedResourcesDirectory)
                    pluginFileJson.set(createdPluginFile.effectiveMain().map {
                        createdPluginFile.validate()
                        val dto = createPluginFileDto(createdPluginFile)
                        GeneratePluginFile.json.encodeToString(dtoSerializer, dto)
                    })
                }

            plugins.withType<JavaPlugin> {
                extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                    resources.srcDir(generateTask)
                }
            }
        }
    }

    private fun Project.registerPluginClasses(pluginClasses: SurfPluginClassTasks, pluginFile: F) {
        pluginFile.generatedMain.convention(pluginClasses.generatedClassName(generatedClass))
        pluginClasses.scanFrom(pluginFile.configuredMain())

        pluginClasses.generateClasses(
            generatedClassDirectory,
            provider {
                if (pluginFile.configuredMain().isPresent) emptyList() else listOf(generatedClass)
            },
        )
        pluginClasses.verifySuperclasses(
            provider {
                pluginFile.configuredMain().orNull
                    ?.let { mapOf(it to generatedClass.superclass) }
                    .orEmpty()
            }
        )
        pluginClasses.forbidBundledSpring()
    }
}
