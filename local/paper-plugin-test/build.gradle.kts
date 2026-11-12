import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `core-convention`

    alias(libs.plugins.plugin.yml.paper)

    id("io.papermc.paperweight.userdev") apply true
}

description = "surf-api-paper-plugin-test"

dependencies {
    compileOnlyApi(projects.surfApiPaperApi)
    // Only for the container host. A plugin starts its own child container through
    // `PaperMain.containers`, and that lives in the implementation module — surf-api's paper-plugin
    // declares `has-open-classloader: false`, so a plugin reaches it exactly because it declares
    // SurfPaperAPI with `joinClasspath = true` below.
    compileOnlyApi(libs.commandapi.paper)

    paperweight.paperDevBundle(libs.paper.api.get().version)
}

paper {
    main = "dev.slne.surf.surfapi.bukkit.test.PaperPluginMain"
    name = "SurfPaperPluginTest"
    description = "Test plugin for Surf API for Paper"
    author = "twisti"
    apiVersion = "1.21"
    foliaSupported = true

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
    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
    }
}
