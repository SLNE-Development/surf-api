package dev.slne.surf.surfapi.gradle.platform.paper.plugin

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.generators.LibrariesLoaderGenerator.generateLibrariesLoaderTask
import dev.slne.surf.surfapi.gradle.platform.paper.AbstractPaperSurfPlugin
import dev.slne.surf.surfapi.gradle.util.registerRequired
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

    private val paperPlugins = listOf(
        "xyz.jpenilla.run-paper",
        "net.minecrell.plugin-yml.paper"
    )

    override fun Project.afterEvaluated0(extension: PaperPluginSurfExtension) {
        val generateLoaderTask = generateLibrariesLoaderTask(
            extension.mainClass.get().substringBeforeLast('.')
        )

        configure<PaperPluginDescription> {
            authors = extension.authors.get()
            main = extension.mainClass.get()
            bootstrapper = extension.bootstrapper.orNull
            loader = generateLoaderTask.get().loaderLocation()
            generateLibrariesJson = true
            apiVersion = Constants.MINECRAFT_VERSION

            bootstrapDependencies {
                registerRequired("surf-bukkit-api")
                extension.bootstrapDependencies.orNull?.execute(this)
            }

            serverDependencies {
                registerRequired("surf-bukkit-api")
                extension.serverDependencies.orNull?.execute(this)
            }
        }

        plugins.withType<JavaPlugin> {
            extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                resources.srcDir(generateLoaderTask)
            }
        }

        tasks {
            withType<RunServer> {
                minecraftVersion(Constants.MINECRAFT_VERSION)

                downloadPlugins {
                    hangar("CommandAPI", "9.7.0")
                    modrinth("luckperms", "v5.4.145-bukkit")
                }

                extension.runServer.orNull?.execute(this)
            }
        }
    }

    override fun createExtension(objects: ObjectFactory) = PaperPluginSurfExtension(objects)

    override fun Project.applyPlugins0() {
        paperPlugins.forEach { plugin ->
            applyPlugin(plugin)
        }
    }
}