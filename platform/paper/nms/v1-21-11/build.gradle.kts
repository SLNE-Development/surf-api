plugins {
    id("surf.module-impl")
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

    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    compileOnly(libs.fastutil)

    compileOnly(libs.placeholder.api)
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
