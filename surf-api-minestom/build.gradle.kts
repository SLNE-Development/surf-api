plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiCore.surfApiCore) {
        exclude(libs.commandapi.core)
        exclude(libs.brigadier)
    }

    implementation(projects.surfApiCore.surfApiCoreServer)
    compileOnlyApi(libs.surf.minestom.lobby.api)

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
    api(libs.inventory.framework.platform.minestom)
    implementation(libs.packetevents.netty.common)
    runtimeOnly(libs.flogger.slf4j.backend)
}

description = "surf-api-minestom"

private fun <T : ModuleDependency> T.exclude(provider: Provider<MinimalExternalModuleDependency>) =
    provider.get().module.apply { exclude(group, name) }

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("it.unimi.dsi.fastutil", "$relocationPrefix.fastutil")
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
    }
}