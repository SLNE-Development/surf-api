import net.minecrell.pluginyml.paper.PaperPluginDescription.DependencyDefinition
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder
import org.gradle.kotlin.dsl.NamedDomainObjectContainerScope

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

    compileOnly(libs.placeholder.api)

    // -------------------- Paper Libraries -------------------- //
    paperLibrary(libs.scoreboard.library.implementation)
    paperLibrary(libs.scoreboard.library.modern)
    paperLibrary(libs.scoreboard.library.api)
    api(libs.inventoryframework)
    api(libs.packetevents.spigot)
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
    paperLibrary(libs.aide.reflection)
    paperLibrary(libs.mccoroutine.folia.api)
    paperLibrary(libs.mccoroutine.folia.core)

    paperLibrary(kotlin("stdlib"))
    paperLibrary(libs.kotlinxCoroutines.core)
    paperLibrary(libs.kotlin.reflect)
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
