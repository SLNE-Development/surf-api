plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi)
    api(projects.surfApiInventoryApi)
    // The configuration domain is no longer re-exported by surf-api-core-api, so a consumer of
    // surf-api-paper reaches the config contracts through here, the way it reaches the
    // shared inventory framework.
    api(projects.surfApiConfigApi)
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.packetevents.spigot)
    compileOnlyApi(libs.commandapi.paper)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)
    api(libs.bundles.inventory.framework)
    api(libs.scoreboard.library.api)
    api(libs.commandapi.bukkit.kotlin)
    compileOnlyApi(libs.mccoroutine.folia.api)
    api(libs.wyck)

    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
}

description = "surf-api-paper-api"

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
