plugins {
    id("dev.slne.surf.surfapi.gradle.core")
    id("dev.slne.surf.microservice") version "1.0.0-SNAPSHOT"
}

dependencies {
    compileOnly("dev.slne.surf.rabbitmq:surf-rabbitmq-server-api:1.0.0-SNAPSHOT")
    api(projects.surfMicroserviceApi)
    api(projects.surfMicroserviceTest.surfMicroserviceTestCore)
    runtimeOnly("dev.slne.surf.rabbitmq:surf-rabbitmq-server:1.0.0-SNAPSHOT")
}