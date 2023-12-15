import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")

    id("xyz.jpenilla.run-paper") version "2.2.2"
    alias(libs.plugins.plugin.yml.paper)
}

description = "surf-api-bukkit-plugin-test"

dependencies {
    compileOnlyApi(project(":surf-api-bukkit-api"))
    compileOnlyApi(libs.commandapi)
}

paper {
    main = "dev.slne.surf.surfapi.bukkit.test.BukkitPluginMain"
    name = "SurfBukkitPluginTest"
    description = "Test plugin for Surf API for Bukkit"
    author = "twisti"
    apiVersion = "1.20"

    serverDependencies {
        register("CommandAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }

        register("SurfBukkitAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}

tasks {
    runServer {
        minecraftVersion("1.20.2")

        downloadPlugins {
            modrinth("commandapi", "9.3.0")
        }
    }
}
