plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi)
    api(projects.surfApiInventoryApi)
    api(projects.surfApiConfigApi)
    api(projects.surfApiSpringApi)
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.packetevents.spigot)
    compileOnlyApi(libs.commandapi.paper)
    compileOnlyApi(libs.more.persistent.data.types)
    compileOnlyApi(libs.bundles.inventory.framework)
    compileOnlyApi(libs.commandapi.bukkit.kotlin)
    compileOnlyApi(libs.wyck)

    testImplementation(libs.paper.api)
    testImplementation(libs.bundles.kotlin.serialization)
}

description = "surf-api-paper-api"

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
