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
    api(projects.surfApiConfigApi)

    compileOnlyApi(libs.minestom.server)
    compileOnlyApi(libs.minestom.brigadier)
    compileOnlyApi(libs.minestom.npc) { exclude(group = "net.minestom") }

    compileOnlyApi(libs.adventure.text.minimessage)

    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.bundles.surf.runtime.utils)
    compileOnlyApi(libs.inventory.framework.platform.minestom)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
}

description = "surf-api-minestom-api"

private fun <T : ModuleDependency> T.exclude(
    provider: Provider<MinimalExternalModuleDependency>,
): ModuleIdentifier {
    return provider.get().module.apply { exclude(group, name) }
}

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
