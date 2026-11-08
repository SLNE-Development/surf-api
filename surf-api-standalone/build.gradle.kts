// Not an API module: this project re-exports the whole standalone platform surface through `api`
// and boots the runtime, so it is an aggregate that happens to carry a bootstrap. Phase 2 splits it
// into an aggregate and an implementation module; until then it keeps the plain core convention,
// because `surf.module-api` rightly rejects its dependency on surf-api-core-server.
plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiCore.surfApiCoreServer) {
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
    api(libs.flogger)
    api(libs.commons.math4.core)
    api(libs.commons.math3)
    implementation(libs.packetevents.netty.common)
    runtimeOnly(libs.flogger.slf4j.backend)
}

private fun <T : ModuleDependency> T.exclude(provider: Provider<MinimalExternalModuleDependency>) =
    provider.get().module.apply { exclude(group, name) }