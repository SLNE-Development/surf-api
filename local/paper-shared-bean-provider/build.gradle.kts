plugins {
    `core-convention`

    id("dev.slne.surf.api.gradle.paper-plugin")
}

description = "surf-api-paper-shared-bean-provider"

dependencies {
    // The plugin supplies `surf-api-paper` and Paper itself as compile-only dependencies. The
    // project dependency is declared as `compileOnlyApi` so the consumer module, which compiles
    // against this one, sees surf-api as well.
    compileOnlyApi(projects.surfApiPaperApi)
}

// The generated bootstrapper and main class sit in this module's package, which is therefore the
// root of the plugin's component scan.
surfPaperPluginApi {
    authors.addAll("SLNE Development Team")
}

paper {
    name = "SurfSharedBeanProvider"
    description = "Exports a @PluginApi bean for surf-api's Spring boot test"
}
