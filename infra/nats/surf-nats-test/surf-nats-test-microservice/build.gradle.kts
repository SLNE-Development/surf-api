plugins {
    id("dev.slne.surf.api.gradle.standalone")
    id("dev.slne.surf.microservice")
}

surfStandaloneApi {
    withSurfNats()
}

surfMicroservice {
    withMicroserviceApi()
}

dependencies {
    implementation(projects.surfNatsTest.surfNatsTestCommon)
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("dev.slne.surf.nats:surf-nats-api")).using(project(":surf-nats-api"))
        substitute(module("dev.slne.surf.nats:surf-nats-standalone")).using(project(":surf-nats-standalone"))
    }
}
