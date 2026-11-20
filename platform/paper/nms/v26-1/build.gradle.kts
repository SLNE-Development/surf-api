plugins {
    id("surf.module-impl")
    id("surf.ksp-processor")
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
    compileOnly(libs.reflection.remapper)

    implementation(libs.bytebuddy)
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
