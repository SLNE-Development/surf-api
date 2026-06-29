import net.minecrell.pluginyml.paper.PaperPluginDescription.DependencyDefinition
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder


plugins {
    `core-convention`

    alias(libs.plugins.plugin.yml.paper)
//    id("io.papermc.paperweight.userdev") apply true
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")
    }
}

dependencies {
    api(projects.surfApiPaper.surfApiPaper)
    api(projects.surfApiCore.surfApiCoreServer)
    api(projects.surfApiPaper.surfApiPaperNms.surfApiPaperNmsCommon)
    compileOnly(libs.paper.api)

    runtimeOnly(projects.surfApiPaper.surfApiPaperNms.surfApiPaperNmsV12111)
    runtimeOnly(projects.surfApiPaper.surfApiPaperNms.surfApiPaperNmsV261)
    runtimeOnly(projects.surfApiPaper.surfApiPaperNms.surfApiPaperNmsV262)

    compileOnly(libs.placeholder.api)

    implementation(libs.guava)
    implementation(libs.caffeine)
    implementation(libs.gson)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)
    implementation(libs.dazzleconf)
    implementation(libs.spongepowered.math)
    implementation(libs.okhttp)
    implementation(libs.fastutil)
    implementation(libs.reflection.remapper)
    implementation(libs.more.persistent.data.types)
    implementation(libs.flogger)
    implementation(libs.flogger.slf4j.backend)
    implementation(libs.commons.math4.core)
    implementation(libs.commons.math3)
    implementation(libs.aide.reflection)
    api(libs.mccoroutine.folia.api)
    api(libs.mccoroutine.folia.core)
    runtimeOnly(libs.scoreboard.library.implementation)
    implementation(libs.scoreboard.library.api)
}

paper {
    name = "surf-paper-api"
    provides = listOf("SurfPaperAPI")
    apiVersion = "26.2"
    description = "Surf API for Paper"
    website = "https://slne.dev"
    authors = listOf("twisti", "SLNE Development Team")
    main = "dev.slne.surf.api.paper.server.PaperMain"

    // Bootstrap
    bootstrapper = "dev.slne.surf.api.paper.server.PaperBoostrapper"
    hasOpenClassloader = false

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
        register("LuckPerms") {
            required = true
            joinClasspath = true
            load = RelativeLoadOrder.BEFORE
        }
    }
}

tasks {
    shadowJar {
        mergeServiceFiles()
        val relocationPrefix: String by project
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
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
