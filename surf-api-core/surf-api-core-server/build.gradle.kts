plugins {
    `core-convention`
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    api(project(":surf-api-shared:surf-api-shared-internal"))
    compileOnly(libs.packetevents.netty.common)
}

description = "surf-api-core-server"
