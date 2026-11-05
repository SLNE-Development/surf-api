package dev.slne.surf.microservice.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Settings of the `generateMicroserviceDockerfile` task.
 */
abstract class MicroserviceDockerExtension {
    /**
     * Destination of the generated Dockerfile. Defaults to `Dockerfile` in the project directory.
     */
    abstract val outputFile: RegularFileProperty

    /**
     * Whether generation may replace a Dockerfile that lacks the generated-file marker. Defaults to `false`.
     */
    abstract val overwrite: Property<Boolean>

    /**
     * Java runtime image of the final container stage.
     */
    abstract val baseImage: Property<String>

    /**
     * JDK image of the Gradle build stage.
     */
    abstract val builderImage: Property<String>

    /**
     * Go toolchain image that compiles `microservicectl`; used only during the Docker build.
     */
    abstract val commandClientBuilderImage: Property<String>
}
