plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}

dependencies {
    implementation("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:8.1.1")
}

gradlePlugin {
    plugins {
        create("dev.slne.surf.microservice") {
            id = "dev.slne.surf.microservice"
            implementationClass = "dev.slne.surf.microservice.plugin.SurfMicroservicePlugin"
        }
    }
}