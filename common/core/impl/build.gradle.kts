plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiInternal)
    api(projects.surfApiCoreApi)
    // The container runs where the implementation runs.
    api(projects.surfApiDiImpl)
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)
}

description = "surf-api-core-impl"
