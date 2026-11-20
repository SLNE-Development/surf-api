package dev.slne.surf.api.gradle.platform.common

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingConfigurer
import dev.slne.surf.api.gradle.util.slnePublic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
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

    private val relocations = mutableListOf<Relocation>()

    init {
        "org.spongepowered.configurate" relocatesTo "configurate"
    }

    protected abstract val extensionClass: Class<E>

    override fun apply(target: Project) = with(target) {
        val extension = extensions.create(
            "surf${platformName.replaceFirstChar { it.uppercase() }}Api",
            extensionClass
        )

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
        relocations += Relocation(this, to)
    }

    protected fun relocatePackage(
        from: String,
        to: String,
        excludes: List<String> = emptyList(),
    ) {
        relocations += Relocation(from, to, excludes)
    }

    /**
     * Relocates [from] below the surf relocation prefix, as `<prefix>.<to>`.
     */
    internal fun applyRelocation(from: String, to: String, excludes: List<String> = emptyList()) {
        relocations += Relocation(from, to, excludes)
    }

    /**
     * Calls [action] for every relocation the shadow jar applies, with [to] already below the surf
     * relocation prefix.
     */
    internal fun forEachRelocation(action: (from: String, to: String, excludes: List<String>) -> Unit) {
        relocations.forEach { action(it.from, "${Constants.RELOCATION_PREFIX}.${it.to}", it.excludes) }
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
            maven("https://repo.papermc.io/repository/maven-public/")
        }

        applyRepositories0()
    }

    protected open fun Project.applyRepositories0() {
    }

    private fun Project.configure() {
        tasks.withType<ShadowJar> {
            relocations.forEach { relocation ->
                relocate(
                    relocation.from,
                    "${Constants.RELOCATION_PREFIX}.${relocation.to}"
                ) {
                    relocation.excludes.forEach { exclude(it) }
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

        configurations.all {
            if (name == "compileOnly") {
                return@all
            }

            dependencies.remove(project.dependencies.gradleApi())
            dependencies.remove(project.dependencies.gradleTestKit())
        }

        configureSurfProcessor()
        configureKotlin()
        configure0()
    }

    private fun Project.configureKotlin() = configure<KotlinJvmProjectExtension> {
        jvmToolchain(Constants.JAVA_VERSION)
        compilerOptions {
            freeCompilerArgs.addAll(
                listOf(
                    "-Xjsr305=strict",
                    "-Xcontext-parameters",
                    "-Xreturn-value-checker=check",
                )
            )
            javaParameters = true
        }
    }

    /**
     * Runs surf-api's KSP processor on the project.
     *
     * The processor generates the NMS provider index and the code behind `@GenerateReflection`.
     * Without it a plugin that declares either compiles and then misses the generated classes at
     * runtime. It collects no bean metadata: Spring finds the beans by scanning the plugin.
     */
    private fun Project.configureSurfProcessor() = dependencies {
        add("ksp", Constants.SURF_PROCESSOR)
    }

    protected open fun Project.configure0() {
    }

    private fun Project.afterEvaluated(extension: E) {
        val testingEnabled = extension.testing.enabled.get()

        if (extension.addSurfApiToClasspath.get()) {
            dependencies {
                val scope = extension.surfApiScope.orNull ?: platform.scope
                add(scope, platform.dependency)
                if (testingEnabled && (scope == COMPILE_ONLY || scope == "compileOnlyApi")) {
                    add(SurfTestingConfigurer.TEST_IMPLEMENTATION, platform.dependency)
                }
            }
        }

        extension.coreModule.orNull?.let {
            dependencies {
                val notation = "dev.slne.surf.core:${it.module}:+"
                add(COMPILE_ONLY, notation)
                if (testingEnabled) {
                    add(SurfTestingConfigurer.TEST_IMPLEMENTATION, notation)
                }
            }
        }

        if (extension.withSurfRedis.get()) {
            dependencies {
                val notation = "dev.slne.surf.api:surf-api-redis-api:${Constants.SURF_API_VERSION}"
                add(COMPILE_ONLY, notation)
                if (testingEnabled) {
                    add(SurfTestingConfigurer.TEST_IMPLEMENTATION, notation)
                }
            }
        }

        if (extension.withApiValidation.get()) {
            configure<KotlinJvmProjectExtension> {
                @OptIn(ExperimentalAbiValidation::class)
                abiValidation {
                    filters {
                        exclude {
                            annotatedWith.add("dev.slne.surf.api.shared.api.util.InternalSurfApi")
                        }
                    }
                }
            }
        }

        if (testingEnabled) {
            SurfTestingConfigurer.configure(this, extension.testing)
            platformTestDependencies(extension)
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

    protected open fun Project.platformTestDependencies(extension: E) {
    }

    private data class Relocation(
        val from: String,
        val to: String,
        val excludes: List<String> = emptyList(),
    )
}