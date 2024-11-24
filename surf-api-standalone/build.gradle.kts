plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-server")) {
        exclude(libs.commandapi.core)
        exclude(libs.brigadier)
    }

    api(libs.adventure.api)
    api(libs.adventure.text.logger.slf4j)
    api(libs.adventure.text.minimessage)
    api(libs.adventure.serializer.gson)
    api(libs.adventure.serializer.legacy)
    api(libs.adventure.serializer.plain)
    api(libs.adventure.serializer.ansi)

    api(libs.guava)
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.okhttp)
    api(libs.fastutil)
    api(libs.configurate.yaml)
    api(libs.configurate.jackson)
    api(libs.flogger)
    api(libs.commons.math4.core)
    implementation(libs.packetevents.netty.common)
    runtimeOnly(libs.flogger.slf4j.backend)
    api(kotlin("stdlib"))
    api(libs.kotlinxCoroutines.core)
}

private fun <T : ModuleDependency> T.exclude(provider: Provider<MinimalExternalModuleDependency>) =
    provider.get().module.apply { exclude(group, name) }