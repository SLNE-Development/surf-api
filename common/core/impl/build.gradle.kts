plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiInternal)
    api(projects.surfApiCoreApi)
    api(projects.surfApiSpringImpl)
    api(projects.surfApiConfigImpl)
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)
}

description = "surf-api-core-impl"
