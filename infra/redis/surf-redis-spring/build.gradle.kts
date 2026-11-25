import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core")
    `api-validation`
}

surfCoreApi {
    allowInternalApi(
        "dev.slne.surf.api.shared.api.util.InternalSurfApi",
        "dev.slne.surf.redis.util.InternalRedisAPI",
    )
}

dependencies {
    api(projects.surfApiRedis.surfApiRedisCore)

    compileOnly(projects.surfApiSpringImpl)

    testImplementation(projects.surfApiSpringImpl)
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

val shadowComponent = components["shadow"] as AdhocComponentWithVariants
shadowComponent.addVariantsFromConfiguration(configurations["sourcesElements"]) {}
shadowComponent.addVariantsFromConfiguration(configurations["javadocElements"]) {}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])
        }
    }

    repositories {
        slneReleases()
    }
}
