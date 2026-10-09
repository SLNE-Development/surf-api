import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

evaluationDependsOn(":surf-nats-paper")

fun surfApiConstant(name: String) = Class.forName("dev.slne.surf.api.gradle.generated.Constants")
    .getField(name)
    .get(null) as String

surfPaperPluginApi {
    mainClass("dev.slne.surf.nats.test.paper.PaperMain")
    authors.addAll("twisti")

    serverDependencies {
        registerRequired("surf-nats-paper")
    }

    runServer {
        downloadPlugins {
            // TODO: 09.10.2026 20:23 - fix luckperms version in surf-api
            modrinth("commandapi", surfApiConstant("COMMAND_API_VERSION"))
            modrinth("luckperms", "v5.5.71-bukkit")
            modrinth("packetevents", "${surfApiConstant("PACKETEVENTS_VERSION")}+spigot")
            hangar("PlaceholderAPI", surfApiConstant("PLACEHOLDER_API_VERSION"))
            val surfApiVersion = surfApiConstant("SURF_API_FULL_VERSION")
            github(
                "SLNE-Development",
                "surf-api",
                "v$surfApiVersion",
                "surf-api-paper-server-$surfApiVersion-all.jar"
            )
        }
        pluginJars(project(":surf-nats-paper").tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    }
}

dependencies {
    compileOnly(projects.surfNatsApi)
    implementation(projects.surfNatsTest.surfNatsTestCommon)
}
