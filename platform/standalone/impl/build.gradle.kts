plugins {
    id("surf.module-impl")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreImpl) {
        exclude(libs.commandapi.core)
        exclude(libs.brigadier)
    }

    api(libs.bundles.adventure)

    api(libs.guava)
    api(libs.bundles.surf.runtime.utils)
    implementation(libs.packetevents.netty.common)
    runtimeOnly(platform(libs.netty.bom))
    runtimeOnly(libs.netty.buffer)
    runtimeOnly(libs.netty.common)
    runtimeOnly(libs.flogger.slf4j.backend)
}

private fun <T : ModuleDependency> T.exclude(
    provider: Provider<MinimalExternalModuleDependency>,
): ModuleIdentifier {
    return provider.get().module.apply { exclude(group, name) }
}