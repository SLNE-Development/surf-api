plugins {
    id("dev.slne.surf.surfapi.gradle.core") version "1.21.11+"
}

group = "dev.slne"
version = findProperty("version") as String


dependencies {
    // Lettuce Redis client
    implementation("io.lettuce:lettuce-core:7.2.1.RELEASE")


//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")


    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
