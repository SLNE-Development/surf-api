package dev.slne.surf.api.gradle.platform.paper.plugin

import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.generators.LibrariesLoaderGenerator.generateLibrariesLoaderTask
import dev.slne.surf.api.gradle.platform.paper.AbstractPaperSurfPlugin
import dev.slne.surf.api.gradle.util.registerRequired
import net.minecrell.pluginyml.GeneratePluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.Project
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

    override val extensionClass = PaperPluginSurfExtension::class.java

    private val paperPlugins = listOf(
        "xyz.jpenilla.run-paper",
        "de.eldoria.plugin-yml.paper"
    )

    override fun Project.afterEvaluated2(extension: PaperPluginSurfExtension) {
        val generateLoaderTask = generateLibrariesLoaderTask(
            extension.mainClass.get().substringBeforeLast('.')
        )

        configure<PaperPluginDescription> {
            authors = extension.authors.get()
            main = extension.mainClass.get()
            bootstrapper = extension.bootstrapper.orNull
            apiVersion = Constants.MINECRAFT_VERSION
            foliaSupported = extension.useCanvasMc.get() || extension.foliaSupported.get()

            if (extension.generateLibraryLoader.get()) {
                generateLibrariesJson = true
                loader = generateLoaderTask.get().loaderLocation()
            }

            bootstrapDependencies {
                registerRequired("surf-paper-api")

                if (extension.withSurfRedis.get() && !extension.surfRedisRelocation.isPresent) {
                    registerRequired("surf-redis-paper")
                }

                extension.bootstrapDependencies.orNull?.execute(this)
            }

            serverDependencies {
                registerRequired("surf-paper-api")
                if (extension.coreModule.isPresent) {
                    registerRequired("surf-core-paper")
                }

                if (extension.withSurfRedis.get() && !extension.surfRedisRelocation.isPresent) {
                    registerRequired("surf-redis-paper")
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

    override fun Project.applyPlugins0() {
        paperPlugins.forEach { plugin ->
            applyPlugin(plugin)
        }
    }
}