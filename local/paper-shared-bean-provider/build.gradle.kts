import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `core-convention`

    alias(libs.plugins.plugin.yml.paper)
}

description = "surf-api-paper-shared-bean-provider"

dependencies {
    // Only compiled against. At runtime surf-api's own paper plugin supplies these classes, and it
    // is reachable exactly because `SurfPaperAPI` is declared below with `joinClasspath = true` --
    // surf-api ships `has-open-classloader: false`, so declaring it is the only way in.
    compileOnlyApi(projects.surfApiPaperApi)
    compileOnly(libs.paper.api)
}

paper {
    name = "SurfSharedBeanProvider"
    main = "dev.slne.surf.api.paper.sharedbean.provider.SharedBeanProviderPlugin"
    description = "Publishes a Scope.SHARED bean for surf-api's shared-bean boot test"
    authors = listOf("SLNE Development Team")
    apiVersion = project.findProperty("mcVersion") as String

    serverDependencies {
        register("SurfPaperAPI") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}
