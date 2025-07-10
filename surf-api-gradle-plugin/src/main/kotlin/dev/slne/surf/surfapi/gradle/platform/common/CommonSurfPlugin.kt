package dev.slne.surf.surfapi.gradle.platform.common

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.util.slnePublic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

abstract class CommonSurfPlugin<E : CommonSurfExtension>(
    protected val platformName: String,
    private val platform: SurfApiPlatform,
) : Plugin<Project> {
    private val commonPlugins = listOf(
        "org.gradle.java-gradle-plugin",
        "org.gradle.java-library",
        "org.gradle.maven-publish",
        "org.jetbrains.kotlin.jvm",
        "org.jetbrains.kotlin.plugin.spring",
        "org.jetbrains.kotlin.plugin.jpa",
        "org.jetbrains.kotlin.plugin.serialization",
        "com.gradleup.shadow",
        "com.google.devtools.ksp"
    )

    private val relocations = mutableMapOf<String, String>()
    private val dependencyDependentRelocations = mutableMapOf<String, MutableMap<String, String>>()

    init {
        addRelocationsForDependency(
            "surf-cloud-api-common",
            "io.netty" to "dev.slne.surf.cloud.netty"
        )
    }

    protected abstract fun createExtension(objects: ObjectFactory, project: Project): E

    override fun apply(target: Project) = with(target) {
        val extension = createExtension(objects, this)
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

    protected fun addRelocationsForDependency(
        dependency: String,
        vararg relocations: Pair<String, String>,
    ) {
        dependencyDependentRelocations.getOrPut(dependency) { mutableMapOf() }.putAll(relocations)
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

            slnePublic()
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

        gradle.projectsEvaluated {
            tasks.withType<ShadowJar> {
                val deps = project.configurations
                    .filter { it.isCanBeResolved }
                    .map { it.incoming.resolutionResult.allDependencies }
                    .flatten()
                    .map { it.requested.displayName }
                    .distinct()
                    .toList()

                println("contains surf-cloud-api-common: ${deps.any { it.contains("surf-cloud-api-common") }}")

                dependencyDependentRelocations.forEach { (dependency, relocations) ->
                    if (deps.any { it.contains(dependency) }) {
                        logger.warn("Dependency $dependency found. Applying relocations.")
                        relocations.forEach { (from, to) ->
                            logger.warn("Relocating $from to $to")
                            relocate(from, to)
                        }
                    }
                }
            }
        }

        configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile> {
            options.encoding = Charsets.UTF_8.name()
            options.compilerArgs.addAll(listOf("-parameters"))
        }

        configureAutoService()
        configureKotlin()
        configureAllOpen()
        configure0()
    }


    private fun Project.configureKotlin() = configure<KotlinJvmProjectExtension> {
        jvmToolchain(Constants.JAVA_VERSION)
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
        }
    }

    private fun Project.configureAllOpen() = configure<AllOpenExtension> {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
    }

    private fun Project.configureAutoService() = dependencies {
        add(COMPILE_ONLY, Constants.AUTO_SERVICE_ANNOTATIONS)
        add("ksp", Constants.AUTO_SERVICE)
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