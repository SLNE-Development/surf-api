@file:OptIn(ExperimentalAbiValidation::class)

import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("dev.slne.surf.api.gradle.core")
}

surfCoreApi {
    withApiValidation()
}

kotlin {
    abiValidation {
        filters {
            exclude {
                annotatedWith.add("dev.slne.surf.redis.util.InternalRedisAPI")
            }
        }
    }
}

dependencies {
    api(libs.redisson) {
        exclude("org.slf4j")
        exclude("org.reactivestreams")
        exclude("io.projectreactor", "reactor-core")
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
        }
    }

    repositories {
        slneReleases()
    }
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = false
}

java {
    withSourcesJar()
    withJavadocJar()
}

/**
 * Only publish the shadow variant
 */
afterEvaluate {
//    tasks.named("publishPluginMavenPublicationToMaven-releasesRepository") {
//        enabled = false
//    }
//    tasks.named("publishPluginMavenPublicationToMavenLocal") {
//        enabled = false
//    }
}
