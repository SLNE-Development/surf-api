package dev.slne.surf.surfapi.gradle.platform.config.paper

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.generators.LibrariesLoaderGenerator.generateLibrariesLoaderTask
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import dev.slne.surf.surfapi.gradle.util.registerRequired
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import org.hibernate.build.publish.auth.maven.MavenRepoAuthExtension
import org.hibernate.build.publish.util.Helper
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import xyz.jpenilla.runpaper.task.RunServer
import java.net.URI
import java.util.*
import javax.inject.Inject

open class Paper @Inject constructor(objects: ObjectFactory) :
    Core(objects, SurfApiPlatform.PAPER) {
    internal val mainClass = objects.property<String>()
    internal val bootstrapper = objects.property<String>().convention(null)
    internal val bootstrapDependencies =
        objects.property<Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>>()
            .convention(null)
    internal val serverDependencies =
        objects.property<Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>>()
            .convention(null)
    internal val runServer = objects.property<Action<RunServer>>().convention(null)

    fun mainClass(mainClass: String) {
        this.mainClass.set(mainClass)
        this.mainClass.finalizeValue()
    }

    fun bootstrapper(bootstrapper: String) {
        this.bootstrapper.set(bootstrapper)
        this.bootstrapper.finalizeValue()
    }

    fun bootstrapDependencies(action: Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>) {
        bootstrapDependencies.set(action)
        bootstrapDependencies.finalizeValue()
    }

    fun serverDependencies(action: Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>) {
        serverDependencies.set(action)
        serverDependencies.finalizeValue()
    }

    fun runServer(action: Action<RunServer>) {
        runServer.set(action)
        runServer.finalizeValue()
    }

    override fun validate0() {
        require(mainClass.isPresent) { "Main class must be set to your plugin's main class" }
    }
}

internal fun Project.applyPaperConfiguration(configuration: Paper) {
    dependencies {
        add(COMPILE_ONLY, Constants.PAPER_API)
    }

    configure<PaperPluginDescription> {
        authors = configuration.authors.get()
        main = configuration.mainClass.get()
        bootstrapper = configuration.bootstrapper.orNull
//            loader // TODO: 01.01.2025 20:51 - auto generate loader
        generateLibrariesJson = true
        apiVersion = Constants.MINECRAFT_VERSION

        bootstrapDependencies {
            registerRequired("surf-bukkit-api")
            configuration.bootstrapDependencies.orNull?.execute(this)
        }

        serverDependencies {
            registerRequired("surf-bukkit-api")
            configuration.serverDependencies.orNull?.execute(this)
        }
    }


    val generateLoaderTask = generateLibrariesLoaderTask(
        configuration.mainClass.get().substringBeforeLast('.')
    )

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

            configuration.runServer.orNull?.execute(this)
        }
    }
}

private fun Project.findLatestVersion(repoId: String, baseUrl: String, prefix: String): String {
    val auth = extensions.getByType<MavenRepoAuthExtension>()
    val provider = auth.credentialsProviderRegistry

    val connection = URI.create(baseUrl).toURL().openConnection().apply {
        val credentials = Helper.locateAuthenticationCredentials(repoId, provider)

        if (credentials != null) {
            setRequestProperty(
                "Authorization",
                "Basic " + Base64.getEncoder()
                    .encodeToString("${credentials.userName}:${credentials.password}".toByteArray())
            )
        }
    }
    val html = connection.getInputStream().bufferedReader().use { it.readText() }

    val versions = Regex("""<a href="(.*?)">""")
        .findAll(html)
        .map { it.groupValues[1].removeSuffix("/") }
        .filter { it.startsWith(prefix) }
        .toList()

    if (versions.isEmpty()) {
        error("No versions found for $prefix")
    }

    val latestVersion = versions.sortedWith { v1, v2 -> compareVersions(v1, v2) }.last()

    return "$baseUrl$latestVersion/$latestVersion-SNAPSHOT.jar"
}

private fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".", "-").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split(".", "-").map { it.toIntOrNull() ?: 0 }
    return parts1.zip(parts2).map { it.first.compareTo(it.second) }.find { it != 0 } ?: 0
}