plugins {
    id("dev.slne.surf.api.gradle.core")
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

dependencies {
    api(projects.surfNatsApi)
    implementation(libs.jnats)

    testImplementation(libs.testcontainers.nats)
    testRuntimeOnly(libs.bundles.log4j)
    testImplementation("dev.slne.surf.api:surf-api-standalone:+") // TODO: fix test dependencies in surf-api
}

tasks.test {
    systemProperty("slf4j.provider", "org.apache.logging.slf4j.SLF4JServiceProvider")
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }
    forClass("dev.slne.surf.nats", "NatsConstants") {
        buildConfigField("VERSION", provider { project.version.toString() })
    }
}