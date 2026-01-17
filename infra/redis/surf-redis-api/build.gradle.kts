import dev.slne.surf.surfapi.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("dev.slne.surf.surfapi.gradle.core")
//    id("dev.slne.surf.surfapi.gradle.standalone") /* Uncomment to use tests */
}

dependencies {
    api("org.redisson:redisson:4.1.0") {
        exclude("org.slf4j")
        exclude("org.reactivestreams")
        exclude("io.projectreactor", "reactor-core")
    }
    implementation("io.netty:netty-transport-native-epoll:4.2.9.Final")

    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.3")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
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

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}