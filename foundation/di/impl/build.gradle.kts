// The container itself: registry, resolver, graph, lifecycle runner and the post-processor chain.
//
// Nothing here touches a platform, so the whole container is exercised by plain JVM tests that
// build their bean definitions in the test source instead of reading them from META-INF.
plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiDiApi)

    // Constructor injection reads the primary constructor and its parameter types.
    api(libs.kotlin.reflect)

    implementation(libs.guava)
}

description = "surf-api-di-impl"
