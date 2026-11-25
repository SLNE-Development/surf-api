import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core")
    `api-validation`
}

surfCoreApi {
    allowInternalApi(
        "dev.slne.surf.api.shared.api.util.InternalSurfApi",
        "dev.slne.surf.nats.InternalNatsApi",
    )
}

dependencies {
    api(projects.surfApiNats.surfApiNatsCore)
    api(projects.surfApiNats.surfApiNatsApi)

    compileOnly(projects.surfApiSpringImpl)
    compileOnly(libs.spring.context)
    compileOnly(libs.spring.boot.health)

    testImplementation(projects.surfApiSpringImpl)
    testImplementation(libs.spring.boot)
    testImplementation(libs.spring.boot.health)
    testImplementation(projects.surfApiNats.surfApiNatsTest.surfApiNatsTestFixtures)
    testImplementation(libs.kotlinx.serialization.protobuf)
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        slneReleases()
    }
}
