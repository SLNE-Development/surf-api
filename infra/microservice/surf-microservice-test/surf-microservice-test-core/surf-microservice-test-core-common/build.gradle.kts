plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}

dependencies {
    api(projects.surfMicroserviceTest.surfMicroserviceTestApi)
    compileOnly("dev.slne.surf.rabbitmq:surf-rabbitmq-client-api:1.0.0-SNAPSHOT")
}
