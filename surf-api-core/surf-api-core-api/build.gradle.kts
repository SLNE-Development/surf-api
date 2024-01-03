plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.adventure.text.minimessage)
    compileOnlyApi("com.github.retrooper.packetevents:api:2.2.0")
    compileOnlyApi(libs.spongepowered.configurate.yaml)
    compileOnlyApi(libs.spongepowered.math)
}

description = "surf-api-core-api"
