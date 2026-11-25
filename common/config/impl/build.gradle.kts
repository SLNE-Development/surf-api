plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiConfigApi)
    implementation(projects.surfApiSpringImpl)
    implementation(libs.bundles.configurate)

    testRuntimeOnly(libs.flogger)
    testRuntimeOnly(libs.flogger.slf4j.backend)
    testRuntimeOnly(libs.adventure.api)
    testRuntimeOnly(libs.adventure.text.minimessage)
    testRuntimeOnly(libs.adventure.text.logger.slf4j)
}

description = "surf-api-config-impl"
