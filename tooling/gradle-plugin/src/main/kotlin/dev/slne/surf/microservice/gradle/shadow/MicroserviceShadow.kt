package dev.slne.surf.microservice.gradle.shadow

import com.github.jengelman.gradle.plugins.shadow.ShadowApplicationPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.util.mergeRegistryFiles
import dev.slne.surf.microservice.gradle.MicroserviceExtension
import dev.slne.surf.microservice.gradle.docker.registerMicroserviceDockerTasks
import dev.slne.surf.microservice.gradle.mainclass.DetectMicroserviceMainClassTask
import dev.slne.surf.microservice.gradle.run.registerRunMicroservice
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

private const val SHADOW_JAR_TASK_NAME = "shadowJar"
private const val DETECT_MAIN_CLASS_TASK_NAME = "detectMicroserviceMainClass"

/**
 * Makes the Shadow jar the runnable microservice and registers the tasks built on it, once Shadow has registered
 * `shadowJar`.
 *
 * Shadow registers `shadowJar` when it applies [ShadowJavaPlugin] in reaction to `java`. Reacting to `java`
 * directly may run before that, depending on the order in which the plugins are applied.
 *
 * This file and [mergeRegistryFiles] hold every reference to Shadow types. Call it only after `com.gradleup.shadow`
 * is applied, so neither class is loaded in builds without Shadow.
 */
internal fun Project.configureMicroserviceShadow(extension: MicroserviceExtension) {
    plugins.withType(ShadowJavaPlugin::class.java) {
        configureShadowJar(extension)
    }
}

private fun Project.configureShadowJar(extension: MicroserviceExtension) {
    val mainClass = registerDetectMainClass(extension)
    val shadowJar = tasks.named<ShadowJar>(SHADOW_JAR_TASK_NAME)
    shadowJar.configure {
        this.mainClass.convention(mainClass)
        mergeRegistryFiles()
    }
    plugins.withType(ShadowApplicationPlugin::class.java) {
        preferDetectedMainClass(shadowJar, mainClass)
    }

    registerMicroserviceDockerTasks(extension.docker, shadowJar)
    registerRunMicroservice(shadowJar)
}

/**
 * Sets the `Main-Class` convention of `shadowJar` to [detected] again, after [ShadowApplicationPlugin] replaced it.
 *
 * Shadow registers `shadowJar.mainClass.convention(application.mainClass)` when the `application` plugin is applied,
 * and the convention registered last wins. Without a main class on `application`, that would leave `shadowJar` with
 * none, whichever plugin was applied first. Running after [ShadowApplicationPlugin] makes the order irrelevant;
 * [detected] already takes the main class of `application` into account.
 *
 * `application.mainClass` itself keeps no convention. [detected] depends on the compiled classes, and the
 * `application` plugin feeds `application.mainClass` into `compileJava`, which would make the task graph circular.
 */
private fun preferDetectedMainClass(shadowJar: TaskProvider<ShadowJar>, detected: Provider<String>) {
    shadowJar.configure { mainClass.convention(detected) }
}

/**
 * The main class the build set on `application`, if it applies the plugin and sets one. Read lazily, because the
 * `application` extension may not exist yet.
 */
private fun Project.applicationMainClass(): Provider<String> =
    providers.provider { extensions.findByType<JavaApplication>()?.mainClass?.orNull }

/**
 * Registers the task that resolves the main class and returns its result.
 *
 * The provider carries the task dependency, so `shadowJar` runs the task before it reads the `Main-Class`.
 */
private fun Project.registerDetectMainClass(extension: MicroserviceExtension): Provider<String> {
    val projectPath = path
    val mainSourceSet = extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME)
    val detect = tasks.register<DetectMicroserviceMainClassTask>(DETECT_MAIN_CLASS_TASK_NAME) {
        group = "build"
        description = "Resolves the main class of the microservice."
        classDirectories.from(mainSourceSet.map { it.output.classesDirs })
        mainClass.set(extension.mainClass.orElse(applicationMainClass()))
        this.projectPath.set(projectPath)
        outputFile.set(layout.buildDirectory.file("generated/surf-microservice/main-class.txt"))
    }
    return detect.flatMap { it.outputFile }.map { it.asFile.readText() }
}
