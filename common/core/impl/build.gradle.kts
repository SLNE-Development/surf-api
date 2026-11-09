plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiInternal)
    api(projects.surfApiCoreApi)
    // The container runs where the implementation runs.
    api(projects.surfApiDiImpl)
    // The configuration domain was cut out of this pair. It is re-exported here because every
    // platform implementation pulls this module in and none of them knows about the cut: this is
    // the one place that keeps the SurfConfigApi bean on their runtime classpath and the config
    // contracts on their compile classpath.
    api(projects.surfApiConfigImpl)
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)
}

description = "surf-api-core-impl"
