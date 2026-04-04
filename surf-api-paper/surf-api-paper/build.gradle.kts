plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiCore.surfApiCore)
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.packetevents.spigot)
    compileOnlyApi(libs.commandapi.paper)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)
    api(libs.bundles.inventory.framework)

    api(libs.commandapi.bukkit.kotlin)
    compileOnlyApi(libs.mccoroutine.folia.api)
}

description = "surf-api-paper"

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
