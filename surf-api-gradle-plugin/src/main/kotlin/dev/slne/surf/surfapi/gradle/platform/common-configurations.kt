package dev.slne.surf.surfapi.gradle.platform

import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfPlugin

fun CommonSurfPlugin<*>.relocateCloudNetty() {
    addRelocationsForDependency(
        "surf-cloud-api-common",
        "io.netty" to "dev.slne.surf.cloud.netty"
    )
}