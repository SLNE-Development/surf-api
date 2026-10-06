plugins {
    id("dev.slne.surf.api.gradle.core")
}

dependencies {
    api(projects.surfNatsApi)
    implementation(libs.jnats)

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.bundles.log4j)
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = false
}
