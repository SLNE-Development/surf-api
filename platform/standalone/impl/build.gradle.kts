plugins {
    id("surf.module-impl")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreImpl) {
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
    api(libs.flogger)
    api(libs.commons.math4.core)
    api(libs.commons.math3)
    implementation(libs.packetevents.netty.common)
    runtimeOnly(platform(libs.netty.bom))
    runtimeOnly(libs.netty.buffer)
    runtimeOnly(libs.netty.common)
    runtimeOnly(libs.flogger.slf4j.backend)
}

private fun <T : ModuleDependency> T.exclude(provider: Provider<MinimalExternalModuleDependency>) =
    provider.get().module.apply { exclude(group, name) }