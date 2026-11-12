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

    paperweight.paperDevBundle("26.3.build.+")

    // CreativeOrderComparator indexes creative-mode items with two maps that have no JDK
    // counterpart: one keyed by Paper's own ItemStackLinkedSet.TYPE_AND_TAG hash strategy, and one
    // keyed by reference identity. The dev bundle happens to carry fastutil, but relying on that is
    // not a dependency declaration.
    compileOnly(libs.fastutil)

    compileOnly(libs.placeholder.api)
    compileOnly(libs.reflection.remapper)
    compileOnly(libs.mccoroutine.folia.api)
    compileOnly(libs.scoreboard.library.api)

    implementation(libs.bytebuddy)
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
