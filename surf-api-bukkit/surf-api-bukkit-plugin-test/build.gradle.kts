import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")

    id("xyz.jpenilla.run-paper") version "2.2.3"
    alias(libs.plugins.plugin.yml.paper)

    id("io.papermc.paperweight.userdev") apply true
}

description = "surf-api-bukkit-plugin-test"

dependencies {
    compileOnlyApi(project(":surf-api-bukkit:surf-api-bukkit-api"))
    compileOnlyApi(libs.commandapi.bukkit)

    paperweight.paperDevBundle(libs.paper.api.get().version)
}

tasks.assemble {
    dependsOn("reobfJar")
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
        minecraftVersion("1.20.4")

        downloadPlugins {
            modrinth("commandapi", "9.4.2")
        }
    }
}
