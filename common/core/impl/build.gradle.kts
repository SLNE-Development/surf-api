plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiInternal)
    api(projects.surfApiCoreApi)
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)
}

description = "surf-api-core-impl"
