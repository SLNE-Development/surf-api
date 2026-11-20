plugins {
    id("surf.module-impl")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

dependencies {
    api(projects.surfApiCoreImpl)
    api(projects.surfApiVelocityApi)
    implementation(shadowed(projects.surfApiRedis.surfApiRedisSpring))
    implementation(projects.surfApiNats.surfApiNatsCore)
    implementation(projects.surfApiNats.surfApiNatsSpring)
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.flogger)
    api(libs.commons.math4.core)
    api(libs.commons.math3)
    api(libs.aide.reflection)
    runtimeOnly(libs.flogger.slf4j.backend)
    kapt(libs.velocity.api)
    compileOnly(libs.velocity.proxy.ctd)

    testImplementation(libs.velocity.api)
}

tasks {
    shadowJar {
        mergeSpringRegistries()
        relocateNats()
    }
}

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

description = "surf-api-velocity-impl"

kapt {
    keepJavacAnnotationProcessors = true
}

buildConfig {
    packageName("dev.slne.surf.api.velocity.server.generated")
    buildConfigField("VERSION", project.version.toString())
}