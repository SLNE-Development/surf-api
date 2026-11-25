plugins {
    id("surf.module-impl")
    `api-validation`
    `java-test-fixtures`
}

dependencies {
    api(projects.surfApiMinestomApi)
    implementation(projects.surfApiCoreImpl)
    implementation(shadowed(projects.surfApiRedis.surfApiRedisSpring))
    implementation(projects.surfApiNats.surfApiNatsCore)
    implementation(projects.surfApiNats.surfApiNatsSpring)
    compileOnlyApi(libs.minestom.server)

    implementation(libs.minestom.brigadier)
    implementation(libs.adventure.text.minimessage)
    implementation(libs.guava)
    implementation(libs.bundles.surf.runtime.utils)
    implementation(libs.inventory.framework.api)
    implementation(libs.inventory.framework.platform.minestom)
    implementation(libs.packetevents.netty.common)
    runtimeOnly(platform(libs.netty.bom))
    runtimeOnly(libs.netty.buffer)
    runtimeOnly(libs.netty.common)
    implementation(libs.sponge.mixin)
    implementation(libs.mixinextras)
    compileOnly(libs.flare.fastutil)
    runtimeOnly(libs.flogger.slf4j.backend)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
    testImplementation(libs.log4j.core)
    testImplementation(libs.log4j.slf4j2.impl)
    testImplementation(libs.minestom.miniplaceholders) { isTransitive = false }

    testFixturesApi(libs.sponge.mixin)
    testFixturesImplementation(libs.bytebuddy.agent)
    testFixturesImplementation(libs.junit.platform.launcher)
}

tasks.shadowJar {
    mergeSpringRegistries()
    excludeSpringBootLogging()
}

val checkContextContributors = tasks.register<CheckShadowServiceFile>("checkContextContributors") {
    group = "verification"
    description = "Checks that the shadow jar lists all bundled ContextContributors."
    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
    service.set("dev.slne.surf.api.spring.context.ContextContributor")
    expectedProviders.addAll(
        "dev.slne.surf.api.config.impl.handle.SurfConfigContributor",
        "dev.slne.surf.api.core.server.impl.reflection.ReflectionProxyContributor",
        "dev.slne.surf.api.nats.NatsContextContributor",
        "dev.slne.surf.api.redis.RedisContextContributor",
    )
}

tasks.check { dependsOn(checkContextContributors) }

description = "surf-api-minestom-impl"

tasks.test {
    systemProperty("minestom.inside-test", "true")
    systemProperty("junit.jupiter.execution.timeout.default", "2m")
}
