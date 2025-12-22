plugins {
    id("dev.slne.surf.surfapi.gradle.core") version "1.21.11+"
}

group = "dev.slne"
version = findProperty("version") as String


dependencies {
    // Lettuce Redis client
    implementation("io.lettuce:lettuce-core:7.2.1.RELEASE")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
