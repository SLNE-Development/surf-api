package dev.slne.surf.surfapi.gradle.platform.paper.plugin

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.generators.LibrariesLoaderGenerator.generateLibrariesLoaderTask
import dev.slne.surf.surfapi.gradle.platform.paper.AbstractPaperSurfPlugin
import dev.slne.surf.surfapi.gradle.util.registerRequired
import dev.slne.surf.surfapi.gradle.util.registerSoft
import net.minecrell.pluginyml.GeneratePluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.runpaper.task.RunServer

internal class PaperPluginSurfPlugin :
    AbstractPaperSurfPlugin<PaperPluginSurfExtension>("paperPlugin") {

    init {
        addRelocationsForDependency(
            "surf-data-api",
            "com.fasterxml.jackson" to "dev.slne.data.libs.jackson"
        )
    }

    private val paperPlugins = listOf(
        "xyz.jpenilla.run-paper",
        "de.eldoria.plugin-yml.paper"
    )

    override fun Project.afterEvaluated0(extension: PaperPluginSurfExtension) {
        val generateLoaderTask = generateLibrariesLoaderTask(
            extension.mainClass.get().substringBeforeLast('.')
        )

        configure<PaperPluginDescription> {
            authors = extension.authors.get()
            main = extension.mainClass.get()
            bootstrapper = extension.bootstrapper.orNull
            apiVersion = Constants.MINECRAFT_VERSION
            foliaSupported = extension.foliaSupported.get()

            if (extension.generateLibraryLoader.get()) {
                generateLibrariesJson = true
                loader = generateLoaderTask.get().loaderLocation()
            }

            bootstrapDependencies {
                registerRequired("surf-bukkit-api")
                if (extension.cloudModule.isPresent) {
                    registerSoft("surf-cloud-bukkit")
                }

                extension.bootstrapDependencies.orNull?.execute(this)
            }

            serverDependencies {
                registerRequired("surf-bukkit-api")
                if (extension.cloudModule.isPresent) {
                    registerSoft("surf-cloud-bukkit")
                }
                extension.serverDependencies.orNull?.execute(this)
            }
        }

        if (extension.generateLibraryLoader.get()) {
            plugins.withType<JavaPlugin> {
                extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                    java.srcDir(generateLoaderTask.map { it.outputDirectory.get() })
                }
            }
        }


        tasks {
            withType<GeneratePluginDescription> {
                useDefaultCentralProxy()
            }

            withType<RunServer> {
                minecraftVersion(Constants.MINECRAFT_VERSION)

                extension.runServer.orNull?.execute(this)
            }
        }
    }

    override fun createExtension(objects: ObjectFactory, project: Project) =
        PaperPluginSurfExtension(objects)

    override fun Project.applyPlugins0() {
        paperPlugins.forEach { plugin ->
            applyPlugin(plugin)
        }
    }
}