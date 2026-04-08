plugins {
    `core-convention`
}

dependencies {
    api(projects.surfApiShared.surfApiSharedInternal)
    api(projects.surfApiCore.surfApiCore)
    implementation("io.ktor:ktor-client-core-jvm:3.4.2")
    implementation("io.ktor:ktor-client-apache:3.4.2")
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)
}

description = "surf-api-core-server"
