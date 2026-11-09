plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi) {
        exclude(libs.commandapi.core)
        exclude(libs.brigadier)
    }
    api(projects.surfApiInventoryApi)

    compileOnlyApi(libs.minestom.server)
    api(libs.minestom.brigadier)
    compileOnlyApi(libs.minestom.npc) { exclude(group = "net.minestom") }

    api(libs.adventure.text.minimessage)

    api(libs.guava)
    api(libs.dazzleconf)
    api(libs.spongepowered.math)
    api(libs.commons.lang3)
    api(libs.commons.text)
    api(libs.okhttp)
    api(libs.flogger)
    api(libs.commons.math4.core)
    api(libs.commons.math3)
    api(libs.inventory.framework.platform.minestom)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
}

description = "surf-api-minestom-api"

private fun <T : ModuleDependency> T.exclude(provider: Provider<MinimalExternalModuleDependency>) =
    provider.get().module.apply { exclude(group, name) }

tasks {
    test {
        systemProperty("minestom.inside-test", "true")
        systemProperty("junit.jupiter.execution.timeout.default", "2m")
    }

    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("me.devnatan.inventoryframework", "$relocationPrefix.devnatan.inventoryframework")
    }
}
