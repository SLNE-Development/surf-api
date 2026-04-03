import net.minecrell.pluginyml.paper.PaperPluginDescription.DependencyDefinition
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder


plugins {
    `core-convention`

    alias(libs.plugins.plugin.yml.paper)
    id("io.papermc.paperweight.userdev") apply true
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.ExperimentalVisualizerApi")
    }
}

dependencies {
    api(projects.surfApiBukkit.surfApiBukkitApi)
    api(projects.surfApiCore.surfApiCoreServer)

    paperweight.paperDevBundle(libs.paper.api.get().version)

    compileOnly(libs.placeholder.api)

    // -------------------- Paper Libraries -------------------- //
    runtimeOnly(libs.scoreboard.library.implementation)
    paperLibrary(libs.scoreboard.library.api)
    paperLibrary(libs.guava)
    paperLibrary(libs.caffeine)
    paperLibrary(libs.gson)
    paperLibrary(libs.commons.lang3)
    paperLibrary(libs.commons.text)
    paperLibrary(libs.dazzleconf)
    paperLibrary(libs.spongepowered.math)
    paperLibrary(libs.okhttp)
    paperLibrary(libs.fastutil)
    paperLibrary(libs.reflection.remapper)
    paperLibrary(libs.configurate.yaml)
    paperLibrary(libs.configurate.jackson)
    paperLibrary(libs.more.persistent.data.types)
    paperLibrary(libs.flogger)
    paperLibrary(libs.flogger.slf4j.backend)
    paperLibrary(libs.commons.math4.core)
    paperLibrary(libs.commons.math3)
    paperLibrary(libs.aide.reflection)
    api(libs.mccoroutine.folia.api)
    api(libs.mccoroutine.folia.core)
}


paper {
    name = "surf-bukkit-api"
    provides = listOf("SurfBukkitAPI")
    apiVersion = "1.21"
    description = "Surf API for Bukkit"
    website = "https://slne.dev"
    authors = listOf("twisti", "SLNE Development Team")
    main = "dev.slne.surf.surfapi.bukkit.server.BukkitMain"

    // Bootstrap
    bootstrapper = "dev.slne.surf.surfapi.bukkit.server.BukkitBoostrapper"
    loader = "dev.slne.surf.surfapi.bukkit.server.BukkitLoader"
    hasOpenClassloader = false
    generateLibrariesJson = true

    // Other
    foliaSupported = true

    // Plugin Dependencies
    serverDependencies {
        registerSoft("ProtocolLib")
        registerSoft("ProtocolSupport")
        registerSoft("ViaVersion")
        registerSoft("ViaBackwards")
        registerSoft("ViaRewind")
        registerSoft("Geyser-Spigot")
        registerSoft("PlaceholderAPI")

        register("CommandAPI") {
            required = true
            joinClasspath = true
            load = RelativeLoadOrder.BEFORE
        }

        register("packetevents") {
            required = true
            joinClasspath = true
            load = RelativeLoadOrder.BEFORE
        }
    }
}

tasks.generatePaperPluginDescription {
    useDefaultCentralProxy()
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}

/**
 * Registers a soft dependency.
 *
 * @param name The name of the dependency.
 * @param required Whether the dependency is required.
 * @param joinClassPath Whether the dependency should be joined to the classpath.
 * @param loadOrder The load order of the dependency.
 */
fun NamedDomainObjectContainerScope<DependencyDefinition>.registerSoft(
    name: String,
    required: Boolean = false,
    joinClassPath: Boolean = true,
    loadOrder: RelativeLoadOrder = RelativeLoadOrder.BEFORE,
) = register(name) {
    this.required = required
    this.joinClasspath = joinClassPath
    this.load = loadOrder
}
