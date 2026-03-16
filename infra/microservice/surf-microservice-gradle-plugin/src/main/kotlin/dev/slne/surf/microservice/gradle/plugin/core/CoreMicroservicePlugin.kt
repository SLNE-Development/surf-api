package dev.slne.surf.microservice.gradle.plugin.core

import dev.slne.surf.microservice.gradle.plugin.CommonMicroservicePlugin

internal class CoreMicroservicePlugin : CommonMicroservicePlugin<CoreMicroserviceExtension>(
    platformName = "core"
) {
    override val extensionClass = CoreMicroserviceExtension::class.java
}