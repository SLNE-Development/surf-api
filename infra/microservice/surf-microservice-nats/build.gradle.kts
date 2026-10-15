plugins {
    id("dev.slne.surf.api.gradle.core")
}

dependencies {
    compileOnly(projects.surfMicroserviceApi.surfMicroserviceApiMicroservice)
    implementation("dev.slne.surf.nats:surf-nats-standalone:+")
}
