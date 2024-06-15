import net.minecrell.pluginyml.paper.PaperPluginDescription.DependencyDefinition
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder

description = "surf-api-bukkit-server"

plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
    alias(libs.plugins.plugin.yml.paper)
    id("io.papermc.paperweight.userdev") apply true
}

tasks.assemble {
    dependsOn("reobfJar")
}

dependencies {
    api(project(":surf-api-bukkit:surf-api-bukkit-api"))
    api(project(":surf-api-core:surf-api-core-server"))

    paperweight.paperDevBundle(libs.paper.api.get().version)

    // -------------------- Paper Libraries -------------------- //
    paperLibrary(libs.scoreboard.library.implementation)
    paperLibrary(libs.scoreboard.library.packetevents)
    paperLibrary(libs.scoreboard.library.modern)
    api(libs.packetevents.spigot)
    // paperLibrary(libs.entitylib)
    paperLibrary(libs.scoreboard.library)
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
}

paper {
    name = "SurfBukkitAPI"
    apiVersion = "1.20"
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
    foliaSupported = false

    // Plugin Dependencies
    serverDependencies {

        registerSoft("ProtocolLib")
        registerSoft("ProtocolSupport")
        registerSoft("ViaVersion")
        registerSoft("ViaBackwards")
        registerSoft("ViaRewind")
        registerSoft("Geyser-Spigot")

        register("CommandAPI") {
            required = true
            joinClasspath = true
            load = RelativeLoadOrder.BEFORE
        }
    }
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
    loadOrder: RelativeLoadOrder = RelativeLoadOrder.BEFORE
) = register(name) {
    this.required = required
    this.joinClasspath = joinClassPath
    this.load = loadOrder
}
