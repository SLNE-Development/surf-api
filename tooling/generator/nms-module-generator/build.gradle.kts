plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("nms-module-generator") {
            id = "dev.slne.surf.api.generator.nms-module-generator"
            implementationClass = "dev.slne.surf.api.generator.nms.GenerateNmsModulePlugin"
        }
    }
}