package dev.slne.surf.microservice.gradle.plugin

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project

enum class SurfMicroserviceModule(
    val apiModule: String,
    val runtimeModule: String,
    val moduleProjectModification: (Project) -> Unit = {}
) {
    COMMON(
        apiModule = "surf-microservice-api-common",
        runtimeModule = "surf-microservice-core"
    ),
    MICROSERVICE(
        apiModule = "surf-microservice-api-microservice",
        runtimeModule = "surf-microservice-microservice",
        moduleProjectModification = { project ->
            project.tasks.named("shadowJar", ShadowJar::class.java) {
                manifest {
                    attributes(
                        mapOf(
                            "Main-Class" to "dev.slne.surf.microservice.runtime.microservice.MicroserviceLauncherKt"
                        )
                    )
                }
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