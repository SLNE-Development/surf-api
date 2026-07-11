plugins {
    `core-convention`
}

dependencies {
    api(projects.surfApiShared.surfApiSharedInternal)
    api(projects.surfApiCore.surfApiCore)
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)

    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.fastutil)
    testRuntimeOnly(libs.adventure.api)
}

description = "surf-api-core-server"
