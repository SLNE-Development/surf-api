package dev.slne.surf.surfapi.gradle.platform.common

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.surfapi.gradle.SurfCloudModules
import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.core.CoreSurfExtension
import dev.slne.surf.surfapi.gradle.platform.core.tasks.generateExposedMigrationScript
import dev.slne.surf.surfapi.gradle.util.slnePublic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.utils.API
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import org.jetbrains.kotlin.gradle.utils.IMPLEMENTATION

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

    fun addRelocationsForDependency(
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
            maven("https://repo.papermc.io/repository/maven-public/")
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

        tasks.withType<ShadowJar>().configureEach {
            val depsProvider = project.provider {
                val deps = project.configurations
                    .asSequence()
                    .filter { it.isCanBeResolved }
                    .flatMap { cfg -> cfg.incoming.resolutionResult.allDependencies.asSequence() }
                    .toList()

                val artifactNames = deps.map { it.requested.displayName }.toSet()

                val projectPaths = deps.mapNotNull { dep ->
                    (dep as? ResolvedDependencyResult)?.selected?.id
                        ?.let { id -> if (id is ProjectComponentIdentifier) id.projectPath else null }
                }.toSet()

                Pair(artifactNames, projectPaths)
            }

            doFirst {
                val (artifactNames, projectPaths) = depsProvider.get()
                dependencyDependentRelocations.forEach { (needle, relos) ->
                    if (artifactNames.any { it.contains(needle) }) {
                        logger.lifecycle("Dependency $needle found — applying relocations.")
                        relos.forEach { (from, to) ->
                            logger.lifecycle("Relocating $from to $to")
                            relocate(from, to)
                        }
                    }
                }

                projectPaths.forEach { projPath ->
                    val depProject = rootProject.findProject(projPath) ?: return@forEach
                    val coreExt = depProject.extensions.findByType(CoreSurfExtension::class.java)
                    if (coreExt != null) {
                        if (coreExt.withSurfDatabaseR2dbc.orNull == true) {
                            coreExt.surfDatabaseR2dbcRelocation.orNull?.let { relocation ->
                                logger.lifecycle("Project dependency $projPath requests DB R2DBC relocation -> $relocation")
                                relocate("dev.slne.surf.database", relocation)
                            }
                        }
                        if (coreExt.withSurfRedis.orNull == true) {
                            coreExt.surfRedisRelocation.orNull?.let { relocation ->
                                logger.lifecycle("Project dependency $projPath requests Redis relocation -> $relocation")
                                relocate("dev.slne.surf.redis", relocation)
                            }
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

        configurations.all {
            if (name == "compileOnly") {
                return@all
            }

            dependencies.remove(project.dependencies.gradleApi())
            dependencies.remove(project.dependencies.gradleTestKit())
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

        val cloudModule = extension.cloudModule.orNull
        if (cloudModule != null) {
            dependencies {
                add(
                    IMPLEMENTATION,
                    platform("dev.slne.surf.cloud:surf-cloud-bom:${Constants.SURF_API_VERSION}")
                )
                add(
                    COMPILE_ONLY,
                    "dev.slne.surf.cloud:${cloudModule.module}:${Constants.SURF_API_VERSION}"
                )
            }

            val mainClass = extension.migrationMainClass.orNull
            if (cloudModule == SurfCloudModules.SERVER && mainClass != null) {
                generateExposedMigrationScript(
                    mainClass = mainClass,
                    cloudRuntimeDependency = "dev.slne.surf.cloud:${SurfCloudModules.STANDALONE.module}:${Constants.SURF_API_VERSION}"
                )
            }
        }

        extension.coreModule.orNull?.let {
            dependencies {
                add(
                    COMPILE_ONLY,
                    "dev.slne.surf.core:${it.module}:${Constants.SURF_API_VERSION}"
                )
            }
        }

        if (extension.withSurfRedis.get()) {
            if (extension.surfRedisRelocation.isPresent) {
                dependencies {
                    add(API, "dev.slne.surf:surf-redis:${extension.surfRedisVersion.get()}")
                }
                tasks.withType<ShadowJar>().configureEach {
                    doFirst {
                        relocate("dev.slne.surf.redis", extension.surfRedisRelocation.get())
                    }
                }
            } else {
                dependencies {
                    add(COMPILE_ONLY, "dev.slne.surf:surf-redis-api:${Constants.SURF_API_VERSION}")
                }
            }
        }

        if (extension.withSurfDatabaseR2dbc.get()) {
            dependencies {
                add(API, "dev.slne.surf:surf-database-r2dbc:${extension.surfDatabaseR2dbcVersion.get()}")
            }

            tasks.withType<ShadowJar>().configureEach {
                doFirst {
                    relocate("dev.slne.surf.database", extension.surfDatabaseR2dbcRelocation.get())
                }
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