plugins {
    id("surf.module-impl")
    id("surf.nms-shared-26")
    id("io.papermc.paperweight.userdev") apply true
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")
    }
}

dependencies {
    api(projects.surfApiPaperNmsApi)
    api(projects.surfApiCoreImpl)

    paperweight.paperDevBundle("26.1+")

    compileOnly(libs.fastutil)

    compileOnly(libs.placeholder.api)
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
