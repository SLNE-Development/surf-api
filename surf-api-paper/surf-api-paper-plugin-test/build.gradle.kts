import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `core-convention`

    alias(libs.plugins.run.paper)
    alias(libs.plugins.plugin.yml.paper)

    id("io.papermc.paperweight.userdev") apply true
}

description = "surf-api-paper-plugin-test"

dependencies {
    compileOnlyApi(projects.surfApiPaper.surfApiPaper)
    compileOnlyApi(libs.commandapi.paper)

    paperweight.paperDevBundle(libs.paper.api.get().version)
}

paper {
    main = "dev.slne.surf.surfapi.bukkit.test.PaperPluginMain"
    name = "SurfPaperPluginTest"
    description = "Test plugin for Surf API for Paper"
    author = "twisti"
    apiVersion = "1.21"

    serverDependencies {
        register("CommandAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }

        register("SurfPaperAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}

tasks {
    runServer {
        dependsOn(":surf-api-paper:surf-api-paper-server:shadowJar")
        pluginJars.from(project(":surf-api-paper:surf-api-paper-server").tasks.shadowJar)

//        minecraftVersion(findProperty("mcVersion") as String)
minecraftVersion("1.21.11")

        downloadPlugins {
            hangar("CommandAPI", libs.versions.commandapi.get())
            modrinth("packetevents", libs.versions.packetevents.plugin.get() + "+spigot")
            modrinth("luckperms", libs.versions.luckpermsplugin.bukkit.get())
        }
    }
}

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
    }
}