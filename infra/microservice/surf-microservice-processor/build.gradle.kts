import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

val kspVersion = "2.3.13"

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation("dev.zacsweers.kctfork:ksp:0.14.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

// kctfork pins slightly older Kotlin and KSP versions; test against the ones microservices build with.
configurations.matching { it.name.startsWith("test") }.configureEach {
    resolutionStrategy.eachDependency {
        when (requested.group) {
            "com.google.devtools.ksp" -> useVersion(kspVersion)
            "org.jetbrains.kotlin" -> useVersion(getKotlinPluginVersion())
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
