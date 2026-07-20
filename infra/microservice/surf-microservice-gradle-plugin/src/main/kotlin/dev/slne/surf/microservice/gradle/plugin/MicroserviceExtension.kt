package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule
import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModuleSettings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/** Configures the Surf microservice Gradle plugin. */
abstract class MicroserviceExtension @Inject constructor(objects: ObjectFactory) {
    internal val module = objects.property<SurfMicroserviceModule>()
    internal val rabbitSettings = objects.property<RabbitModuleSettings>()

    /** Dockerfile generation settings for Coolify and other container platforms. */
    val docker: MicroserviceDockerExtension = objects.newInstance()

    /** Configures [docker]. */
    fun docker(action: Action<in MicroserviceDockerExtension>) {
        action.execute(docker)
    }

    fun withRabbitModule(
        rabbitModule: RabbitModule,
        applyRabbitServerRuntimeDependency: Boolean = false,
        applyRabbitKspProcessor: Boolean = true
    ) {
        this.rabbitSettings.set(
            RabbitModuleSettings(
                rabbitModule,
                applyRabbitServerRuntimeDependency,
                applyRabbitKspProcessor
            )
        )

        this.rabbitSettings.finalizeValue()
    }

    fun withMicroserviceApi() {
        this.module.set(SurfMicroserviceModule.MICROSERVICE)
        this.module.finalizeValue()
    }

    fun withCommonApi() {
        this.module.set(SurfMicroserviceModule.COMMON)
        this.module.finalizeValue()
    }

    fun withClientCommonApi() {
        this.module.set(SurfMicroserviceModule.CLIENT_COMMON)
        this.module.finalizeValue()
    }

    fun withClientPaperApi() {
        this.module.set(SurfMicroserviceModule.CLIENT_PAPER)
        this.module.finalizeValue()
    }

    fun withClientVelocityApi() {
        this.module.set(SurfMicroserviceModule.CLIENT_VELOCITY)
        this.module.finalizeValue()
    }
}

/** Settings used by the `generateMicroserviceDockerfile` task. */
abstract class MicroserviceDockerExtension @Inject constructor(objects: ObjectFactory) {
    /** Destination of the generated Dockerfile. Defaults to `Dockerfile` in the project directory. */
    val outputFile: RegularFileProperty = objects.fileProperty()

    /** Whether generation may replace a Dockerfile that lacks the generated-file marker. */
    val overwrite: Property<Boolean> = objects.property<Boolean>()

    /** Java runtime image used by the final container stage. */
    val baseImage: Property<String> = objects.property<String>()

    /** Java development-kit image used by the Gradle build stage. */
    val builderImage: Property<String> = objects.property<String>()
}
