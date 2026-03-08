plugins {
    id("dev.slne.surf.surfapi.gradle.standalone")
    id("dev.slne.surf.microservice") version "1.0.0-SNAPSHOT"
}

dependencies {
    api(projects.surfMicroserviceApi)
}