plugins {
    id("dev.slne.surf.api.gradle.core")
}

dependencies {
    api(projects.surfNatsApi)
    implementation(libs.jnats)

    testImplementation(libs.testcontainers.nats)
    testRuntimeOnly(libs.bundles.log4j)
    testImplementation("dev.slne.surf.api:surf-api-standalone:+") // TODO: fix test dependencies in surf-api
}

tasks.test {
    failOnNoDiscoveredTests = false
    systemProperty("slf4j.provider", "org.apache.logging.slf4j.SLF4JServiceProvider")
}
