import dev.slne.surf.surfapi.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.surfapi.gradle.core") version "1.21.11+"
//    id("dev.slne.surf.surfapi.gradle.standalone") version "1.21.11+" /* Uncomment to use tests */
}

group = "dev.slne.surf"
version = findProperty("version") as String

dependencies {
    implementation(libs.bundles.exposed) {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-reactive")
        exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
    }
    implementation(libs.r2dbc.pool)
    implementation(libs.bundles.databaseDriver)
}

configurations.all {
    exclude("io.projectreactor", "reactor-core")
    exclude("org.reactivestreams")
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

afterEvaluate {
    tasks.named("publishPluginMavenPublicationToMaven-releasesRepository") {
        enabled = false
    }
    tasks.named("publishPluginMavenPublicationToMavenLocal") {
        enabled = false
    }
}