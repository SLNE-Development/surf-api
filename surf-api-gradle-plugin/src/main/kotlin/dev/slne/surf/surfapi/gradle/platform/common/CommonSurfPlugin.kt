package dev.slne.surf.surfapi.gradle.platform.common

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal abstract class CommonSurfPlugin<E : CommonSurfExtension>(
    private val platformName: String,
    private val platform: SurfApiPlatform,
) : Plugin<Project> {

    private val repos = mapOf<String, String>(
        "maven-unsafe" to "https://repo.slne.dev/repository/maven-unsafe/",
        "maven-public" to "https://repo.slne.dev/repository/maven-public/",
        "maven-snapshots" to "https://repo.slne.dev/repository/maven-snapshots",
        "maven-proxy" to "https://repo.slne.dev/repository/maven-proxy",
        "maven-external-developers" to "https://repo.slne.dev/repository/maven-external-developers",
    )

    private val commonPlugins = listOf(
        "org.gradle.java-gradle-plugin",
        "org.gradle.java-library",
        "org.gradle.maven-publish",
        "org.jetbrains.kotlin.jvm",
        "org.jetbrains.kotlin.kapt",
        "org.jetbrains.kotlin.plugin.spring",
        "org.jetbrains.kotlin.plugin.jpa",
        "org.hibernate.build.maven-repo-auth",
        "com.gradleup.shadow"
    )

    private val relocations = mutableMapOf<String, String>()

    protected abstract fun createExtension(objects: ObjectFactory): E

    override fun apply(target: Project) = target.run {
        // Add extension
        val extension = createExtension(objects)
        extensions.add("surf${platformName.replaceFirstChar { it.uppercase() }}Api", extension)

        applyRepositories()
        applyPlugins()
        configure(extension)

        afterEvaluate {
            extension.validate()
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

            for ((name, url) in repos) {
                maven(url) { this.name = name }
            }
        }

        applyRepositories0()
    }

    protected open fun Project.applyRepositories0() {
    }

    private fun Project.configure(extension: E) {
        tasks.withType<ShadowJar> {
            relocations.forEach { (from, to) ->
                relocate(from, "${Constants.RELOCATION_PREFIX}.$to")
            }
        }

        configureAutoService()
        configureKotlin(extension)
        configure0()
    }

    private fun Project.configureKotlin(extension: E) = configure<KotlinJvmProjectExtension> {
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
        setProperty("kotlin.stdlib.default.dependency", extension.shadeKotlin.get())

        afterEvaluated0(extension)
    }

    protected open fun Project.afterEvaluated0(extension: E) {
    }
}