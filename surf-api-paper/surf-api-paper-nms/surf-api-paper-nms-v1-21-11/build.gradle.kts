plugins {
    `core-convention`
    id("io.papermc.paperweight.userdev") apply true
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")
    }
}

dependencies {
    api(projects.surfApiPaper.surfApiPaperNms.surfApiPaperNmsCommon)
    api(projects.surfApiCore.surfApiCoreServer)

    paperweight.paperDevBundle(libs.canvas.api.get().version)

    compileOnly(libs.placeholder.api)
    compileOnly(libs.reflection.remapper)
    compileOnly(libs.mccoroutine.folia.api)
    compileOnly(libs.scoreboard.library.api)

    implementation(libs.bytebuddy)
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
