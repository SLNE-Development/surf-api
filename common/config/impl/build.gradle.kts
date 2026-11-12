// The runtime half of the configuration domain: the `SurfConfigApi` bean and the per-`Class`
// registry of config managers it hands out.
plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiConfigApi)
}

description = "surf-api-config-impl"
