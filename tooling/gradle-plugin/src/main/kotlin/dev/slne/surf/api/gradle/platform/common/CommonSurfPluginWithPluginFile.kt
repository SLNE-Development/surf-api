package dev.slne.surf.api.gradle.platform.common

import dev.slne.surf.api.gradle.generators.GeneratePluginFile
import dev.slne.surf.api.gradle.generators.pluginfiles.CommonPluginFile
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import kotlinx.serialization.SerializationStrategy
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.kotlin.dsl.withType

abstract class CommonSurfPluginWithPluginFile<E : CommonSurfExtension, F : CommonPluginFile, D>(
    platformName: String,
    platform: SurfApiPlatform,
    private val pluginFileName: String,
) : CommonSurfPlugin<E>(platformName, platform) {

    protected abstract val dtoSerializer: SerializationStrategy<D>

    protected abstract fun createPluginFile(project: Project): F
    protected abstract fun createPluginFileDto(pluginFile: F): D

    override fun apply(target: Project) {
        super.apply(target)

        with(target) {
            val generatedResourcesDirectory =
                layout.buildDirectory.dir("generated/surf-api/$platformName")

            val createdPluginFile = createPluginFile(this)

            val generateTask =
                tasks.register<GeneratePluginFile>("generate${platformName.uppercaseFirstChar()}PluginFile") {
                    group = "surf-api"

                    fileName.set(pluginFileName)
                    outputDir.set(generatedResourcesDirectory)
                    pluginFileJson.set(provider {
                        if (createdPluginFile.isApplied()) {
                            createdPluginFile.validate()
                            val dto = createPluginFileDto(createdPluginFile)
                            GeneratePluginFile.json.encodeToString(dtoSerializer, dto)
                        } else {
                            logger.warn(
                                "Plugin file generation is skipped because the plugin file is not applied. " +
                                        "Please check if the plugin file is applied in the build.gradle.kts file."
                            )
                            ""
                        }
                    })
                }

            plugins.withType<JavaPlugin> {
                extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                    resources.srcDir(generateTask)
                }
            }
        }
    }
}