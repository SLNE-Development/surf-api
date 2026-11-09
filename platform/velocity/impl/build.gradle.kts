plugins {
    id("surf.module-impl")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

dependencies {
    api(projects.surfApiCoreImpl)
    api(projects.surfApiVelocityApi)
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.okhttp)
    api(libs.flogger)
    api(libs.commons.math4.core)
    api(libs.commons.math3)
    api(libs.aide.reflection)
    runtimeOnly(libs.flogger.slf4j.backend)
    kapt(libs.velocity.api)
    compileOnly(libs.velocity.proxy.ctd)

    // The proxy is provided at runtime, so it is compileOnly -- but a test that stands in for it
    // has to load its interfaces, and `compileOnly` does not reach the test classpath.
    testImplementation(libs.velocity.api)
}

tasks {
    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
    }
}

description = "surf-api-velocity-impl"

kapt {
    keepJavacAnnotationProcessors = true
}

buildConfig {
    packageName("dev.slne.surf.api.velocity.server.generated")
    buildConfigField("VERSION", project.version.toString())
}