package dev.slne.surf.surfapi.gradle.platform.config.paper

import dev.slne.surf.surfapi.gradle.Constants
import dev.slne.surf.surfapi.gradle.Versions
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import dev.slne.surf.surfapi.gradle.util.registerRequired
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.hibernate.build.publish.auth.maven.MavenRepoAuthExtension
import org.hibernate.build.publish.util.Helper
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import xyz.jpenilla.runpaper.task.RunServer
import java.net.URI
import java.util.*

class Paper : Core(SurfApiPlatform.PAPER) {
    var mainClass: String? = null
    var bootstrapper: String? = null

    override fun validate0() {
        requireNotNull(mainClass) { "Main class must be set to your plugin's main class" }
    }
}

internal fun Project.applyPaperConfiguration(configuration: Paper) {
    dependencies {
        add(COMPILE_ONLY, Constants.PAPER_API)
    }

    configure<PaperPluginDescription> {
        authors = configuration.authors
        main = configuration.mainClass
        bootstrapper = configuration.bootstrapper
//            loader // TODO: 01.01.2025 20:51 - auto generate loader
        generateLibrariesJson = true
        apiVersion = Versions.MINECRAFT_VERSION

        bootstrapDependencies {
            registerRequired("surf-bukkit-api")
        }

        serverDependencies {
            registerRequired("surf-bukkit-api")
        }
    }

    tasks {
        withType<RunServer> {
            minecraftVersion(Versions.MINECRAFT_VERSION)

            downloadPlugins {
//                url(
//                    findLatestVersion(
//                        Constants.SNAPSHOT_REPO_ID,
//                        Constants.SURF_API_BUKKIT_SERVER_URL,
//                        Versions.API_VERSION.removeSuffix("+")
//                    )
//                )

                hangar("CommandAPI", "9.7.0")
                modrinth("luckperms", "v5.4.145-bukkit")
            }
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