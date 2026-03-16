package dev.slne.surf.microservice.gradle.plugin.api

import dev.slne.surf.microservice.gradle.plugin.CommonMicroservicePlugin
import org.gradle.api.Project

internal class ApiMicroservicePlugin : CommonMicroservicePlugin<ApiMicroserviceExtension>(
    platformName = "api"
) {
    override val extensionClass = ApiMicroserviceExtension::class.java

    override fun Project.afterEvaluate0(extension: ApiMicroserviceExtension) {
        
    }
}