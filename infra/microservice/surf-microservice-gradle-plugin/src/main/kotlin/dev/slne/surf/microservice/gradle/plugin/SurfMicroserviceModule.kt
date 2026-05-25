package dev.slne.surf.microservice.gradle.plugin

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

enum class SurfMicroserviceModule(
    internal val apiModule: String,
    internal val runtimeModule: String,
    internal val moduleProjectModification: Project.() -> Unit = {}
) {
    COMMON(
        apiModule = "surf-microservice-api-common",
        runtimeModule = "surf-microservice-core"
    ),
    MICROSERVICE(
        apiModule = "surf-microservice-api-microservice",
        runtimeModule = "surf-microservice-microservice",
        moduleProjectModification = {
            tasks.withType<ShadowJar>().configureEach {
                mainClass.set("dev.slne.surf.microservice.runtime.microservice.MicroserviceLauncherKt")
            }
        }
    ),
    CLIENT_COMMON(
        apiModule = "surf-microservice-api-client-common",
        runtimeModule = "surf-microservice-client-common"
    ),
    CLIENT_PAPER(
        apiModule = "surf-microservice-api-client-paper",
        runtimeModule = "surf-microservice-client-paper"
    ),
    CLIENT_VELOCITY(
        apiModule = "surf-microservice-api-client-velocity",
        runtimeModule = "surf-microservice-client-velocity"
    );
}