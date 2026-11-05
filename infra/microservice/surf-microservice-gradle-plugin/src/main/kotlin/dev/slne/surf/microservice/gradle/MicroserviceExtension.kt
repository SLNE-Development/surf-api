package dev.slne.surf.microservice.gradle

import org.gradle.api.Action
import org.gradle.api.tasks.Nested

/**
 * Configures the surf microservice plugin, registered as `surfMicroservice`.
 */
abstract class MicroserviceExtension {
    @get:Nested
    abstract val database: MicroserviceDatabaseExtension

    @get:Nested
    abstract val docker: MicroserviceDockerExtension

    fun database(action: Action<in MicroserviceDatabaseExtension>) {
        action.execute(database)
    }

    fun docker(action: Action<in MicroserviceDockerExtension>) {
        action.execute(docker)
    }
}
