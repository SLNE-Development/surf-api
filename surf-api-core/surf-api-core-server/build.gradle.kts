plugins {
    `core-convention`
}

dependencies {
    api(projects.surfApiShared.surfApiSharedInternal)
    api(projects.surfApiCore.surfApiCoreApi)
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)
}

description = "surf-api-core-server"
