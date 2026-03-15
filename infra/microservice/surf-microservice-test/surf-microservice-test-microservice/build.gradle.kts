plugins {
    id("dev.slne.surf.surfapi.gradle.core")
    id("dev.slne.surf.microservice") version "1.0.0-SNAPSHOT"
}

surfCoreApi {
    withSurfDatabaseR2dbc("1.3.0", "dev.slne.surf.microservice.test.microservice.libs")
}

dependencies {
    compileOnly("dev.slne.surf.rabbitmq:surf-rabbitmq-server-api:1.0.0-SNAPSHOT")
    runtimeOnly("dev.slne.surf.rabbitmq:surf-rabbitmq-server:1.0.0-SNAPSHOT")
    api(projects.surfMicroserviceApi)
    api(projects.surfMicroserviceTest.surfMicroserviceTestCore.surfMicroserviceTestCoreCommon)
}