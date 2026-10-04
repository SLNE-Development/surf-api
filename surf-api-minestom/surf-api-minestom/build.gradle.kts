plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiCore.surfApiCore) {
        exclude(libs.commandapi.core)
        exclude(libs.brigadier)
    }

    compileOnlyApi(libs.minestom.server)
    api(libs.minestom.brigadier)
    compileOnlyApi(libs.minestom.npc)

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

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
}

description = "surf-api-minestom"

private fun <T : ModuleDependency> T.exclude(provider: Provider<MinimalExternalModuleDependency>) =
    provider.get().module.apply { exclude(group, name) }

tasks {
    test {
        systemProperty("minestom.inside-test", "true")
    }

    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("it.unimi.dsi.fastutil", "$relocationPrefix.fastutil")
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
    }
}
