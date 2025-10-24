import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `core-convention`

    alias(libs.plugins.run.paper)
    alias(libs.plugins.plugin.yml.paper)

    id("io.papermc.paperweight.userdev") apply true
}

description = "surf-api-bukkit-plugin-test"

dependencies {
    compileOnlyApi(project(":surf-api-bukkit:surf-api-bukkit-api"))
    compileOnlyApi(libs.commandapi.paper)

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
    apiVersion = "1.21"

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
        dependsOn(":surf-api-bukkit:surf-api-bukkit-server:reobfJar")
        pluginJars.from(project(":surf-api-bukkit:surf-api-bukkit-server").tasks.reobfJar)

        minecraftVersion(findProperty("mcVersion") as String)

        downloadPlugins {
            hangar("CommandAPI", libs.versions.commandapi.get())
//            modrinth("packetevents", libs.versions.packetevents.plugin.get() + "spigot")
            url("https://ci.codemc.io/job/retrooper/job/packetevents/770/artifact/build/libs/packetevents-spigot-2.10.0-SNAPSHOT.jar")
        }
    }
}

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
    }
}