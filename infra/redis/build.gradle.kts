plugins {
    id("dev.slne.surf.surfapi.gradle.core") version "1.21.11+"
//    id("dev.slne.surf.surfapi.gradle.standalone") version "1.21.11+" /* Uncomment to use tests */
}

group = "dev.slne"
version = findProperty("version") as String


dependencies {
    // Lettuce Redis client
    implementation("io.lettuce:lettuce-core:7.2.1.RELEASE")

    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.3")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
