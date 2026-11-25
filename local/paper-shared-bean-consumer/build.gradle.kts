import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `core-convention`

    alias(libs.plugins.plugin.yml.paper)
}

description = "surf-api-paper-shared-bean-consumer"

/**
 * What this jar carries beyond its own classes: the generated NATS contracts of the fixtures, which
 * the probes call and serve. Everything else they need, Spring and surf-api included, is supplied
 * by surf-api's plugin at runtime, so the configuration is not transitive.
 */
val bundled = configurations.create("bundled") {
    isCanBeConsumed = false
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

dependencies {
    compileOnlyApi(projects.surfApiPaperApi)
    compileOnly(libs.paper.api)

    // The provider's interface, which has to stay *out* of this jar. At runtime it comes from the
    // providing plugin's own jar through Paper's class loader group, which is what `SharedGreeting`
    // being loaded exactly once depends on.
    compileOnly(projects.surfApiPaperSharedBeanProvider)

    // Referenced only from beans behind @ConditionalOnPlugin. PlaceholderAPI is absent on the test
    // server, so its adapter must be skipped without ever loading a class of it.
    compileOnly(libs.placeholder.api)
    compileOnly(libs.luckperms)

    // The Redis and NATS integrations ship inside surf-api's plugin. The probes compile against the
    // shaded Redis api, the one whose Redisson packages are the relocated ones that plugin carries.
    compileOnly(shadowed(projects.surfApiRedis.surfApiRedisApi))
    compileOnly(projects.surfApiNats.surfApiNatsApi)
    compileOnly(projects.surfApiNats.surfApiNatsTypes.surfApiNatsTypesPaper)
    compileOnly(libs.kotlinx.serialization.protobuf)

    bundled(projects.surfApiNats.surfApiNatsTest.surfApiNatsTestFixtures)
    compileOnly(projects.surfApiNats.surfApiNatsTest.surfApiNatsTestFixtures)
}

/**
 * The jar the test server runs: this plugin's classes and [bundled], with Configurate relocated
 * like in every plugin the surf Gradle plugin builds, so `@ConfigSerializable` is the annotation
 * surf-api's plugin reads.
 */
tasks.shadowJar {
    configurations.set(listOf(bundled))
}

paper {
    name = "SurfSharedBeanConsumer"
    main = "dev.slne.surf.api.paper.sharedbean.consumer.SharedBeanConsumerPlugin"
    // Deliberately in the parent package of both plugins: the scan starts there and must still
    // pick up only this jar's components, not the provider's.
    bootstrapper = "dev.slne.surf.api.paper.sharedbean.SharedBeanConsumerBootstrapper"
    description = "Injects another plugin's @PluginApi bean for surf-api's Spring boot test"
    authors = listOf("SLNE Development Team")
    apiVersion = project.findProperty("mcVersion") as String

    bootstrapDependencies {
        register("SurfPaperAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
        // The provider's context, and with it the export, must exist before this one is built.
        register("SurfSharedBeanProvider") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }

    serverDependencies {
        register("SurfPaperAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
        // `joinClasspath` is what puts the provider's loader into this plugin's group, so
        // `SharedGreeting` resolves to the provider's copy.
        register("SurfSharedBeanProvider") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
        // The command bean extends CommandAPICommand; Paper does not pass CommandAPI's classes on
        // through surf-api, so the consumer joins CommandAPI itself, as every plugin with commands
        // does.
        register("CommandAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
        register("LuckPerms") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}
