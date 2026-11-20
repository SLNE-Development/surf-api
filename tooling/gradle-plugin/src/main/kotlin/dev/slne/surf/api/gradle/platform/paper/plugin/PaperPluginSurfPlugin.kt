package dev.slne.surf.api.gradle.platform.paper.plugin

import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.generators.LibrariesLoaderGenerator.generateLibrariesLoaderTask
import dev.slne.surf.api.gradle.generators.PaperLibraryLoaderClassName
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingConfigurer
import dev.slne.surf.api.gradle.platform.paper.AbstractPaperSurfPlugin
import dev.slne.surf.api.gradle.pluginclass.GeneratedPluginClass
import dev.slne.surf.api.gradle.pluginclass.SurfPluginClassTasks
import dev.slne.surf.api.gradle.util.registerRequired
import net.minecrell.pluginyml.GeneratePluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.NamedDomainObjectContainerScope
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

/**
 * The name CommandAPI has as a Paper plugin, as surf-api's own plugin declares it.
 */
private const val COMMAND_API_PLUGIN = "CommandAPI"

/**
 * The task plugin-yml writes the Paper plugin description with.
 */
private const val PAPER_DESCRIPTION_TASK = "generatePaperPluginDescription"

internal class PaperPluginSurfPlugin :
    AbstractPaperSurfPlugin<PaperPluginSurfExtension>("paperPlugin") {

    override val extensionClass = PaperPluginSurfExtension::class.java

    private val paperPlugins = listOf(
        "de.eldoria.plugin-yml.paper"
    )

    private lateinit var pluginClasses: SurfPluginClassTasks

    override fun apply(target: Project) {
        super.apply(target)

        val extension = target.extensions.getByType(extensionClass)
        pluginClasses = SurfPluginClassTasks(target, extension, SurfApiPlatform.PAPER)

        pluginClasses.generateClasses(
            "paper-plugin",
            target.provider {
                listOfNotNull(
                    GeneratedPluginClass.PAPER_BOOTSTRAPPER.takeUnless { extension.bootstrapper.isPresent },
                    GeneratedPluginClass.PAPER_PLUGIN.takeUnless { extension.mainClass.isPresent },
                )
            },
        )
        pluginClasses.scanFrom(extension.bootstrapper)
        pluginClasses.generatesLibrariesLoader(extension.generateLibraryLoader)
        pluginClasses.verifySuperclasses(
            target.provider {
                buildMap {
                    extension.mainClass.orNull?.let { put(it, GeneratedPluginClass.PAPER_PLUGIN.superclass) }
                    extension.bootstrapper.orNull?.let { put(it, GeneratedPluginClass.PAPER_BOOTSTRAPPER.superclass) }
                }
            }
        )
        pluginClasses.forbidBundledSpring()
    }

    override fun Project.afterEvaluated2(extension: PaperPluginSurfExtension) {
        val generateLoaderTask = generateLibrariesLoaderTask(pluginClasses.basePackage)

        if (extension.installSurfNpc.get()) {
            val notation = "dev.slne.surf.npc:surf-npc-api:+"
            dependencies.add(COMPILE_ONLY, notation)
            if (extension.testing.enabled.get()) {
                dependencies.add(SurfTestingConfigurer.TEST_IMPLEMENTATION, notation)
            }
        }

        configure<PaperPluginDescription> {
            authors = extension.authors.get()
            apiVersion = Constants.MINECRAFT_VERSION
            foliaSupported = extension.useCanvasMc.get() || extension.foliaSupported.get()

            if (extension.generateLibraryLoader.get()) {
                generateLibrariesJson = true
            }

            bootstrapDependencies {
                registerRequired("surf-paper-api")

                extension.bootstrapDependencies.orNull?.execute(this)
                registerSurfPluginDependencies(extension)
            }

            serverDependencies {
                registerRequired("surf-paper-api")
                if (extension.coreModule.isPresent) {
                    registerRequired("surf-core-paper")
                }

                if (extension.installSurfNpc.get()) {
                    registerRequired("surf-npc-paper")
                }

                extension.serverDependencies.orNull?.execute(this)
                registerSurfPluginDependencies(extension)

                // Paper does not hand CommandAPI's classes on through surf-api.
                if (findByName(COMMAND_API_PLUGIN) == null) {
                    registerRequired(COMMAND_API_PLUGIN)
                }
            }
        }

        if (extension.generateLibraryLoader.get()) {
            plugins.withType<JavaPlugin> {
                extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                    java.srcDir(generateLoaderTask.flatMap { it.outputDirectory })
                }
            }
        }

        tasks {
            withType<GeneratePluginDescription> {
                useDefaultCentralProxy()
            }
        }

        fillClassNamesLazily(extension)
    }

    /**
     * Registers every surf plugin of [extension] with joinClasspath and load before. Called for the
     * server and the bootstrap dependencies alike, after the build script's own dependencies, which
     * win when they declare the same plugin.
     */
    private fun NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>.registerSurfPluginDependencies(
        extension: PaperPluginSurfExtension,
    ) {
        extension.surfPluginDependencies.get().filter { findByName(it.name) == null }.forEach { dependency ->
            register(dependency.name) {
                required = dependency.required
                joinClasspath = true
                load = RelativeLoadOrder.BEFORE
            }
        }
    }

    /**
     * Sets `main`, `bootstrapper` and `loader` of the Paper description when plugin-yml reads it
     * for `generatePaperPluginDescription`. The generated names depend on the base package, which
     * only exists once the sources have been read; deriving the description from it makes the task
     * depend on that computation.
     *
     * Replaces the description provider of plugin-yml, because a provider derived from the property
     * it replaces is circular. The provider therefore applies the same defaults as plugin-yml.
     */
    private fun Project.fillClassNamesLazily(extension: PaperPluginSurfExtension) {
        val paperDescription = extensions.getByType<PaperPluginDescription>()
        val projectName = provider { name }
        val projectVersion = provider { version.toString() }
        val projectDescription = provider { description }
        val website = provider { findProperty("url")?.toString() }
        val author = provider { findProperty("author")?.toString() }

        val mainClass = extension.mainClass.orElse(pluginClasses.generatedClassName(GeneratedPluginClass.PAPER_PLUGIN))
        val bootstrapper = extension.bootstrapper
            .orElse(pluginClasses.generatedClassName(GeneratedPluginClass.PAPER_BOOTSTRAPPER))
        val generateLibraryLoader = extension.generateLibraryLoader

        tasks.withType<GeneratePluginDescription>().configureEach {
            if (name != PAPER_DESCRIPTION_TASK) return@configureEach

            // plugin-yml reads the description as a nested bean, so Gradle does not trace its provider.
            pluginClasses.dependOnBasePackage(this)
            pluginDescription.set(pluginClasses.basePackage.map { basePackage ->
                paperDescription.name = paperDescription.name ?: projectName.get()
                paperDescription.version = paperDescription.version ?: projectVersion.get()
                paperDescription.description = paperDescription.description ?: projectDescription.orNull
                paperDescription.website = paperDescription.website ?: website.orNull
                paperDescription.author = paperDescription.author ?: author.orNull

                paperDescription.main = mainClass.orNull
                paperDescription.bootstrapper = bootstrapper.orNull
                if (generateLibraryLoader.get()) {
                    paperDescription.loader = "$basePackage.$PaperLibraryLoaderClassName"
                }
                paperDescription
            })
        }
    }

    override fun Project.applyPlugins0() {
        paperPlugins.forEach { plugin ->
            applyPlugin(plugin)
        }
    }
}
