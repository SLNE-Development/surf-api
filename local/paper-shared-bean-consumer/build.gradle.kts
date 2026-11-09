import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `core-convention`

    alias(libs.plugins.plugin.yml.paper)
}

description = "surf-api-paper-shared-bean-consumer"

dependencies {
    // Only compiled against: surf-api's own paper plugin supplies these at runtime.
    compileOnlyApi(projects.surfApiPaperApi)
    compileOnly(libs.paper.api)

    // The provider's interface, which has to stay *out* of this jar. At runtime it comes from the
    // providing plugin's own jar through Paper's class loader group, which is what `SharedGreeting`
    // being loaded exactly once depends on -- bundling a second copy here is the failure mode
    // `ClassLoaderIsolationTest` records.
    compileOnly(projects.surfApiPaperSharedBeanProvider)
}

paper {
    name = "SurfSharedBeanConsumer"
    main = "dev.slne.surf.api.paper.sharedbean.consumer.SharedBeanConsumerPlugin"
    description = "Injects another plugin's Scope.SHARED bean for surf-api's shared-bean boot test"
    authors = listOf("SLNE Development Team")
    apiVersion = project.findProperty("mcVersion") as String

    serverDependencies {
        register("SurfPaperAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }

        // `joinClasspath` is what puts the provider's loader into this plugin's group, so
        // `SharedGreeting` resolves to the provider's copy; `BEFORE` is what makes the provider's
        // container -- and therefore the shared bean -- exist by the time this plugin loads.
        register("SurfSharedBeanProvider") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}
