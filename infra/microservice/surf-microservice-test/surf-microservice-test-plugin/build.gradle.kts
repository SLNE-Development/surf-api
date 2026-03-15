import dev.slne.surf.surfapi.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.test.microservice.TestPaperMain")
    serverDependencies {
        registerRequired("surf-rabbitmq-paper")
    }
}

dependencies {
    compileOnly("dev.slne.surf.rabbitmq:surf-rabbitmq-client-api:1.0.0-SNAPSHOT")
    api(projects.surfMicroserviceTest.surfMicroserviceTestCore)
}