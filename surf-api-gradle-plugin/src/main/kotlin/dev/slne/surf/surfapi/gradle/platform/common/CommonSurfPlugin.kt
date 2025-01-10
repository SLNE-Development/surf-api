package dev.slne.surf.surfapi.gradle.platform.common

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.util.*
import groovy.lang.MissingPropertyException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

abstract class CommonSurfPlugin<E : CommonSurfExtension>(
    private val platformName: String,
    private val platform: SurfApiPlatform,
) : Plugin<Project> {
    private val commonPlugins = listOf(
        "org.gradle.java-gradle-plugin",
        "org.gradle.java-library",
        "org.hibernate.build.maven-repo-auth",
        "org.gradle.maven-publish",
        "org.jetbrains.kotlin.jvm",
        "org.jetbrains.kotlin.kapt",
        "org.jetbrains.kotlin.plugin.spring",
        "org.jetbrains.kotlin.plugin.jpa",
        "com.gradleup.shadow"
    )

    private val relocations = mutableMapOf<String, String>()

    protected abstract fun createExtension(objects: ObjectFactory): E

    override fun apply(target: Project) = with(target) {
        val extension = createExtension(objects)
        extensions.add("surf${platformName.replaceFirstChar { it.uppercase() }}Api", extension)

        applyRepositories()
        applyPlugins()
        configure()
        setupPublication()

        afterEvaluate {
            try {
                extension.validate()
            } catch (e: Exception) {
                logger.error(
                    "Failed to validate extension. Please check your configuration and try again.",
                    e
                )
                return@afterEvaluate
            }
            afterEvaluated(extension)
        }
    }

    protected infix fun String.relocatesTo(to: String) {
        relocations[this] = to
    }

    private fun Project.applyPlugins() {
        commonPlugins.forEach { pluginId ->
            applyPlugin(pluginId)
        }

        applyPlugins0()
    }

    protected open fun Project.applyPlugins0() {
    }

    protected fun Project.applyPlugin(pluginId: String) {
        try {
            pluginManager.apply(pluginId)
        } catch (e: Exception) {
            logger.error("Failed to apply plugin: $pluginId", e)
        }
    }

    private fun Project.applyRepositories() {
        repositories {
            mavenCentral()
            gradlePluginPortal()

            slneUnsafe()
            slnePublic()
            slneSnapshots()
            slneProxy()
            slneExternalDevelopers()
        }

        applyRepositories0()
    }

    protected open fun Project.applyRepositories0() {
    }

    private fun Project.configure() {
        tasks.withType<ShadowJar> {
            relocations.forEach { (from, to) ->
                relocate(from, "${Constants.RELOCATION_PREFIX}.$to")
            }
        }

        configureAutoService()
        configureKotlin()
        configure0()
    }

    private fun Project.configureKotlin() = configure<KotlinJvmProjectExtension> {
        jvmToolchain(Constants.JAVA_VERSION)
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
        }
    }

    private fun Project.configureAutoService() = dependencies {
        add(COMPILE_ONLY, Constants.AUTO_SERVICE_ANNOTATIONS)
        add("kapt", Constants.AUTO_SERVICE)
    }

    protected open fun Project.configure0() {
    }

    private fun Project.afterEvaluated(extension: E) {
        if (extension.addSurfApiToClasspath.get()) {
            dependencies {
                val scope = extension.surfApiScope.orNull ?: platform.scope
                add(scope, platform.dependency)
            }
        }
        try {
            setProperty("kotlin.stdlib.default.dependency", extension.shadeKotlin.get())
        } catch (_: MissingPropertyException) {
            logger.warn("Failed to set shadeKotlin property! Maybe the Kotlin plugin is not applied?")
        }

        afterEvaluated0(extension)
    }

    private fun Project.setupPublication() = configure<PublishingExtension> {
        publications {
//            publications.create<MavenPublication>("maven") {
//                from(components["java"])
//            }
        }
    }


    protected open fun Project.afterEvaluated0(extension: E) {
    }
}