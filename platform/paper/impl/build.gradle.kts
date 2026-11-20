import net.minecrell.pluginyml.paper.PaperPluginDescription.DependencyDefinition
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder


plugins {
    id("surf.module-impl")

    alias(libs.plugins.plugin.yml.paper)
//    id("io.papermc.paperweight.userdev") apply true
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")
    }
}

dependencies {
    api(projects.surfApiPaperApi)
    api(projects.surfApiCoreImpl)
    api(projects.surfApiPaperNmsApi)
    // The Redis integration, shipped inside this plugin already relocated.
    implementation(shadowed(projects.surfApiRedis.surfApiRedisSpring))
    // The NATS integration, shipped inside this plugin; io.nats is relocated by this jar.
    implementation(projects.surfApiNats.surfApiNatsCore)
    implementation(projects.surfApiNats.surfApiNatsSpring)
    implementation(projects.surfApiNats.surfApiNatsTypes.surfApiNatsTypesPaper)
    compileOnly(libs.paper.api)

    // The server provides Paper's api at runtime, so it is compileOnly -- but a test that stands in
    // for the server has to load those interfaces, and `compileOnly` does not reach the test
    // classpath.
    testImplementation(libs.paper.api)

    runtimeOnly(projects.surfApiPaperNmsV12111)
    runtimeOnly(projects.surfApiPaperNmsV261)
    runtimeOnly(projects.surfApiPaperNmsV262)
    runtimeOnly(projects.surfApiPaperNmsV263)

    compileOnly(libs.placeholder.api)

    // Paper provides Guava, Gson and SLF4J, and its plugin class loader asks the server first.
    compileOnly(libs.guava)
    implementation(libs.caffeine)
    compileOnly(libs.gson)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)
    implementation(libs.dazzleconf)
    implementation(libs.spongepowered.math)
    implementation(libs.reflection.remapper)
    implementation(libs.more.persistent.data.types)
    implementation(libs.flogger)
    implementation(libs.flogger.slf4j.backend)
    implementation(libs.commons.math4.core)
    implementation(libs.commons.math3)
    implementation(libs.aide.reflection)
}

paper {
    name = "surf-paper-api"
    provides = listOf("SurfPaperAPI")
    apiVersion = "26.3"
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
        // NATS ships unrelocated in its modules; only the plugin jar relocates it.
        relocateNats()
        dependencies {
            // Provided by the server, which the plugin class loader asks first.
            exclude(dependency("org.slf4j:slf4j-api:.*"))
        }
        // Spring's own registries, which every Spring jar carries a part of.
        mergeSpringRegistries()
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
        relocate("dev.wyck", "$relocationPrefix.dev.wyck")
    }
}

// The merged service file must keep every bundled ContextContributor; a lost one only shows as missing beans at startup.
val checkContextContributors by tasks.registering(CheckShadowServiceFile::class) {
    group = "verification"
    description = "Checks that the shadow jar lists the config, NATS and Redis ContextContributors."
    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
    service.set("dev.slne.surf.api.spring.context.ContextContributor")
    expectedProviders.addAll(
        "dev.slne.surf.api.config.impl.handle.SurfConfigContributor",
        "dev.slne.surf.api.nats.NatsContextContributor",
        "dev.slne.surf.api.redis.RedisContextContributor",
    )
}

tasks.check { dependsOn(checkContextContributors) }

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
