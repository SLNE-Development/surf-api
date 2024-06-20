plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    paperweight.paperDevBundle(libs.paper.api.get().version)
    compileOnlyApi(libs.packetevents.spigot)
    compileOnlyApi(libs.scoreboard.library)
    compileOnlyApi(libs.commandapi.bukkit)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)
    api(libs.com.github.stefvanschie.inventoryframework.`if`)
    api(libs.commandapi.bukkit.kotlin)
}

description = "surf-api-bukkit-api"
