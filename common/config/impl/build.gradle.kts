// The runtime half of the configuration domain: the `SurfConfigApi` bean, the per-`Class`
// registry of config managers it hands out, and the `ConfigHandle` of every `@SurfConfig` class.
plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiConfigApi)
    // Contributes the config handles to every plugin context.
    implementation(projects.surfApiSpringImpl)

    // The platforms provide these at runtime.
    testRuntimeOnly(libs.flogger)
    testRuntimeOnly(libs.flogger.slf4j.backend)
    testRuntimeOnly(libs.adventure.api)
    testRuntimeOnly(libs.adventure.text.minimessage)
    testRuntimeOnly(libs.adventure.text.logger.slf4j)
}

description = "surf-api-config-impl"
