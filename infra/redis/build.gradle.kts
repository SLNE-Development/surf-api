plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
}

group = "de.slne"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Lettuce Redis client
    implementation("io.lettuce:lettuce-core:6.3.0.RELEASE")
    
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
