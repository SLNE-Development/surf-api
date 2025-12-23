import dev.slne.surf.surfapi.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.surfapi.gradle.core") version "1.21.11+"
//    id("dev.slne.surf.surfapi.gradle.standalone") version "1.21.11+" /* Uncomment to use tests */
}

group = "dev.slne.surf"
version = findProperty("version") as String


dependencies {
    implementation("io.lettuce:lettuce-core:7.2.1.RELEASE") {
        exclude("org.slf4j")
        exclude("org.reactivestreams")
        exclude("io.projectreactor", "reactor-core")
    }

    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.3")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

tasks.shadowJar {
    relocationPrefix = "dev.slne.surf.redis.libs"
    enableAutoRelocation = true
}

shadow {
    addShadowVariantIntoJavaComponent = false
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

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

afterEvaluate {
    tasks.named("publishPluginMavenPublicationToMaven-releasesRepository") {
        enabled = false
    }
    tasks.named("publishPluginMavenPublicationToMavenLocal") {
        enabled = false
    }
}
