package dev.slne.surf.microservice.gradle.shadow

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.microservice.gradle.MicroserviceDockerExtension
import dev.slne.surf.microservice.gradle.docker.registerMicroserviceDockerTasks
import dev.slne.surf.microservice.gradle.run.registerRunMicroservice
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.kotlin.dsl.named

/**
 * JVM binary name of the launcher's top-level `main` in `surf-microservice-runtime`.
 */
internal const val MICROSERVICE_MAIN_CLASS = "dev.slne.surf.microservice.runtime.MicroserviceLauncherKt"

private const val SHADOW_JAR_TASK_NAME = "shadowJar"

/**
 * Makes the Shadow jar the runnable microservice and registers the tasks built on it, once Shadow has registered
 * `shadowJar`.
 *
 * Shadow registers `shadowJar` when it applies [ShadowJavaPlugin] in reaction to `java`; reacting to `java`
 * directly may run before that, depending on the order in which the plugins are applied.
 *
 * Holds every reference to Shadow types. Call it only after `com.gradleup.shadow` is applied, so this class is
 * never loaded in builds without Shadow.
 */
internal fun Project.configureMicroserviceShadow(docker: MicroserviceDockerExtension) {
    plugins.withType(ShadowJavaPlugin::class.java) {
        configureShadowJar(docker)
    }
}

private fun Project.configureShadowJar(docker: MicroserviceDockerExtension) {
    val shadowJar = tasks.named<ShadowJar>(SHADOW_JAR_TASK_NAME)
    shadowJar.configure {
        mainClass.set(MICROSERVICE_MAIN_CLASS)
        mergeServiceFiles()
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    registerMicroserviceDockerTasks(docker, shadowJar)
    registerRunMicroservice(shadowJar)
}
