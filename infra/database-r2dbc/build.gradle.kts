import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core") version "+"
}

surfCoreApi {
    withApiValidation()
}

group = "dev.slne.surf"
version = findProperty("version") as String

dependencies {
    implementation(libs.bundles.exposed)
    implementation(libs.r2dbc.pool)
    implementation(libs.bundles.databaseDriver)
}

configurations.runtimeClasspath {
    exclude("io.projectreactor", "reactor-core")
    exclude("org.reactivestreams")
    exclude("org.slf4j")
    exclude("org.jetbrains.kotlin")
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-reactive")
    exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
}

shadow {
    addShadowVariantIntoJavaComponent = false
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    shadowJar {
        relocationPrefix = "dev.slne.surf.database.libs"
        enableAutoRelocation = true
    }

    test {
        useJUnitPlatform()
    }
}

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


/**
 * Only publish shadow variant
 */
afterEvaluate {
    tasks.named("publishPluginMavenPublicationToSlne-repository-releasesRepository") {
        enabled = false
    }
    tasks.named("publishPluginMavenPublicationToMavenLocal") {
        enabled = false
    }
}