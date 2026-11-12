package dev.slne.surf.nats.gradle

import dev.slne.surf.nats.gradle.bridge.SurfApiBridge
import dev.slne.surf.nats.gradle.generator.DefaultTypeMappings
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.TypeMapping
import dev.slne.surf.nats.gradle.tasks.CheckSurfNatsCompatibilityTask
import dev.slne.surf.nats.gradle.tasks.FetchSurfNatsBaselineTask
import dev.slne.surf.nats.gradle.tasks.GenerateSurfNatsKotlinTask
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.addTypeDependenciesToPublish
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.applyWeaver
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.baselineName
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.guardOfflineFetch
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.publishedModule
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.stageWeaverSource
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.validateWeaverCoordinates
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.weaverImportPath
import dev.slne.surf.nats.gradle.weaver.WeaverIntegration.wirePublishProtos
import dev.slne.weaver.gradle.WeaverExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Generates Kotlin contracts from `.proto` files and wires them into a Kotlin/JVM build.
 *
 * Apply the plugin with the id [ID] to every module that uses surf-nats or contains `.proto` contracts:
 *
 * ```kotlin
 * plugins {
 *     kotlin("jvm") version "KOTLIN_VERSION"
 *     id("dev.slne.surf.api.gradle.nats") version "SURF_API_VERSION"
 * }
 *
 * surfNats {
 *     generatedVisibility = INTERNAL
 * }
 * ```
 *
 * Replace `KOTLIN_VERSION` with your Kotlin version and `SURF_API_VERSION` with the surf-api release. Configure
 * the plugin through [SurfNatsExtension] (`surfNats { }`).
 *
 * The plugin registers these tasks:
 *
 * - [GENERATE_TASK] ([GenerateSurfNatsKotlinTask]): writes the contracts to `build/generated/surf-nats/kotlin`.
 * - [STAGE_TASK]: copies the `.proto` files of [SurfNatsExtension.protoSourceDirs] to
 *   `build/surf-nats/weaver-source`, the directory weaver publishes.
 * - [BASELINE_TASK] ([FetchSurfNatsBaselineTask]): downloads the previous weaver release of the module.
 * - [CHECK_TASK] ([CheckSurfNatsCompatibilityTask]): compares the sources with that release.
 *
 * With `org.jetbrains.kotlin.jvm`, the generated directory is added to the `main` source set, the Kotlin
 * serialization plugin is applied, and `check` depends on [CHECK_TASK]. Applying `org.jetbrains.kotlin.multiplatform`
 * fails the build with a [GradleException].
 *
 * The plugin also applies `dev.slne.weaver` with these conventions, which `weaver { }` overrides:
 *
 * - `url`: the `WEAVER_URL` environment variable, otherwise `https://weaver.slne.dev`.
 * - `repositories` and `publishing.repository`: `proto-releases-public`.
 * - `publishing.group`: `project.group` when set explicitly.
 * - `publishing.name`: `rootProject.name`, for published modules only.
 *
 * A *published module* applies `maven-publish` and configures at least one publishing repository. For published
 * modules `publish` first runs weaver's `publishProtos`, after code generation and the compatibility check, and the
 * Maven publications run after it. The plugin validates their weaver coordinates after all projects are evaluated
 * and fails when two projects of the build publish the same `group:name`. For other modules `publish` does not run
 * `publishProtos`, and the compatibility check runs only with [SurfNatsExtension.compatibilityBaseline] set.
 * Dependencies in the `weaver` configuration are import paths for generation and are not generated themselves.
 *
 * When a surf-api Gradle plugin (`core`, `paper-plugin`, `paper-raw`, `velocity`, `minestom`, `minestom-server`
 * or `standalone`) is applied, the plugin adds `surf-api-nats-api`, `surf-api-nats-types-common` and
 * `kotlinx-serialization-protobuf` to `compileOnly` and `testImplementation`. The surf-nats modules are pinned
 * strictly to the plugin version. On top of that:
 *
 * - `paper-plugin` and `paper-raw` also get `surf-api-nats-types-paper` and its default type mappings.
 * - `paper-plugin` declares `surf-api-nats-paper` as a required bootstrap and server dependency in `paper-plugin.yml`.
 * - `velocity` declares `surf-api-nats-velocity` as a required plugin dependency.
 * - `standalone` applies `dev.slne.surf.api.gradle.microservice` and adds `surf-api-nats-standalone` to
 *   `implementation`.
 */
class SurfNatsPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        val extension = createExtension()
        val defaultTypeMappings = objects.listProperty(TypeMapping::class.java)
            .value(DefaultTypeMappings.common)
        val typeModules = objects.listProperty(String::class.java)
            .value(listOf(TYPES_COMMON_MODULE))

        val weaver = applyWeaver()
        stageWeaverSource(extension, weaver, STAGE_TASK, GROUP)
        guardOfflineFetch()
        addTypeDependenciesToPublish(typeModules)
        validateWeaverCoordinates(extension, weaver)

        val active = extension.compatibilityBaseline.map { true }.orElse(publishedModule())

        val generateTask = registerGenerateTask(extension, weaver, defaultTypeMappings)
        val baselineTask = registerBaselineTask(extension, weaver, active)
        val compatibilityTask = registerCompatibilityTask(extension, weaver, baselineTask, active)
        wirePublishProtos(generateTask, compatibilityTask)

        configureKotlinPlugins(generateTask, compatibilityTask)
        configureSurfApiPlugins(defaultTypeMappings, typeModules)
    }

    private fun Project.createExtension(): SurfNatsExtension {
        val extension = extensions.create(
            EXTENSION,
            SurfNatsExtension::class.java,
        )

        extension.protoSourceDirs.convention(layout.projectDirectory.dir("src/main/proto"))
        extension.generatedVisibility.convention(GeneratedVisibility.PUBLIC)
        extension.compatibilityCheck.convention(true)

        return extension
    }

    private fun Project.registerGenerateTask(
        extension: SurfNatsExtension,
        weaver: WeaverExtension,
        defaultTypeMappings: ListProperty<TypeMapping>,
    ): TaskProvider<GenerateSurfNatsKotlinTask> =
        tasks.register(GENERATE_TASK, GenerateSurfNatsKotlinTask::class.java) {
            this.group = GROUP
            this.description = "Generates Kotlin contracts from the surf-nats .proto sources."

            this.sourceDirectories.from(extension.protoSourceDirs)
            this.protoPathDirectories.from(extension.protoPath)
            this.weaverProtoPath.from(weaverImportPath(weaver))

            this.defaultTypeMappings.set(defaultTypeMappings)
            this.typeMappings.set(extension.typeMappings)
            this.visibility.set(extension.generatedVisibility)
            this.outputDir.set(layout.buildDirectory.dir("generated/surf-nats/kotlin"))
        }

    private fun Project.registerBaselineTask(
        extension: SurfNatsExtension,
        weaver: WeaverExtension,
        active: Provider<Boolean>,
    ): TaskProvider<FetchSurfNatsBaselineTask> =
        tasks.register(BASELINE_TASK, FetchSurfNatsBaselineTask::class.java) {
            this.group = GROUP
            this.description = "Downloads the previous weaver release of this module as the compatibility baseline."

            this.url.set(weaver.url)
            this.token.set(weaver.token)
            this.repository.set(weaver.publishing.repository)
            this.artifactGroup.set(weaver.publishing.group)
            this.artifactName.set(baselineName(weaver))
            this.projectVersion.set(weaver.publishing.version)
            this.requestedVersion.set(extension.compatibilityBaseline)
            this.active.set(extension.compatibilityCheck.zip(active) { enabled, needed -> enabled && needed })
            this.offline.set(gradle.startParameter.isOffline)
            this.outputDir.set(layout.buildDirectory.dir("surf-nats/baseline/protos"))
            this.versionFile.set(layout.buildDirectory.file("surf-nats/baseline/version.txt"))
        }

    private fun Project.registerCompatibilityTask(
        extension: SurfNatsExtension,
        weaver: WeaverExtension,
        baselineTask: TaskProvider<FetchSurfNatsBaselineTask>,
        active: Provider<Boolean>,
    ): TaskProvider<CheckSurfNatsCompatibilityTask> =
        tasks.register(CHECK_TASK, CheckSurfNatsCompatibilityTask::class.java) {
            this.group = LifecycleBasePlugin.VERIFICATION_GROUP
            this.description = "Checks the surf-nats .proto sources against the previous weaver release."

            this.sourceDirectories.from(extension.protoSourceDirs)
            this.protoPathDirectories.from(extension.protoPath)
            this.weaverProtoPath.from(weaverImportPath(weaver))

            this.baselineDirectory.from(baselineTask.flatMap { it.outputDir })
            this.baselineVersionFile.set(baselineTask.flatMap { it.versionFile })
            this.active.set(active)
            this.checkEnabled.set(extension.compatibilityCheck)
            this.reportFile.set(layout.buildDirectory.file("reports/surf-nats/compatibility.txt"))
        }

    private fun Project.configureKotlinPlugins(
        generateTask: TaskProvider<GenerateSurfNatsKotlinTask>,
        compatibilityTask: TaskProvider<CheckSurfNatsCompatibilityTask>,
    ) {
        pluginManager.withPlugin(KOTLIN_JVM) {
            extensions.getByType(KotlinJvmProjectExtension::class.java)
                .sourceSets.named("main") {
                    this.kotlin.srcDir(generateTask.flatMap { it.outputDir })
                }

            pluginManager.apply(KOTLIN_SERIALIZATION)

            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
                this.dependsOn(compatibilityTask)
            }
        }

        pluginManager.withPlugin(KOTLIN_MULTIPLATFORM) {
            throw GradleException(
                "$ID supports Kotlin/JVM projects only; $path applies $KOTLIN_MULTIPLATFORM"
            )
        }
    }

    private fun Project.configureSurfApiPlugins(
        defaultTypeMappings: ListProperty<TypeMapping>,
        typeModules: ListProperty<String>,
    ) {
        var paperTypesAdded = false

        for (pluginId in SURF_API_PLUGINS) {
            pluginManager.withPlugin(pluginId) {
                val isPaper = pluginId in SURF_API_PAPER_PLUGINS

                if (isPaper && !paperTypesAdded) {
                    paperTypesAdded = true
                    defaultTypeMappings.addAll(DefaultTypeMappings.paper)
                    typeModules.add(TYPES_PAPER_MODULE)
                }

                with(SurfApiBridge) {
                    addContractDependencies(isPaper)
                }
            }
        }

        pluginManager.withPlugin(SURF_API_PAPER_PLUGIN) {
            with(SurfApiBridge) {
                requirePaperPlugin()
            }
        }

        pluginManager.withPlugin(SURF_API_VELOCITY) {
            with(SurfApiBridge) {
                requireVelocityPlugin()
            }
        }

        pluginManager.withPlugin(SURF_API_STANDALONE) {
            configureMicroservice()
        }
    }

    /**
     * Turns a surf-api standalone project into a microservice: applies the surf-microservice plugin and adds the
     * NATS runtime at the version of this plugin, which starts through the microservice integration SPI.
     */
    private fun Project.configureMicroservice() {
        pluginManager.apply(MICROSERVICE_PLUGIN)

        val runtime = dependencies.create("${SurfNatsPluginVersion.GROUP}:$STANDALONE_MODULE") as ExternalModuleDependency
        runtime.version { strictly(SurfNatsPluginVersion.VERSION) }
        dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, runtime)
    }

    companion object {
        /**
         * Plugin id.
         */
        const val ID = "dev.slne.surf.api.gradle.nats"

        /**
         * Name of the [SurfNatsExtension] in the build script.
         */
        const val EXTENSION = "surfNats"

        /**
         * Name of the [GenerateSurfNatsKotlinTask].
         */
        const val GENERATE_TASK = "generateSurfNatsKotlin"

        /**
         * Name of the [CheckSurfNatsCompatibilityTask].
         */
        const val CHECK_TASK = "checkSurfNatsCompatibility"

        /**
         * Name of the `Sync` task that stages the proto sources for weaver.
         */
        const val STAGE_TASK = "stageSurfNatsWeaverSource"

        /**
         * Name of the [FetchSurfNatsBaselineTask].
         */
        const val BASELINE_TASK = "fetchSurfNatsBaseline"

        private const val GROUP = "surf-nats"
        private const val TYPES_COMMON_MODULE = "surf-api-nats-types-common"
        private const val TYPES_PAPER_MODULE = "surf-api-nats-types-paper"
        private const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        private const val KOTLIN_MULTIPLATFORM = "org.jetbrains.kotlin.multiplatform"
        private const val KOTLIN_SERIALIZATION = "org.jetbrains.kotlin.plugin.serialization"

        private const val SURF_API_PAPER_PLUGIN = "dev.slne.surf.api.gradle.paper-plugin"
        private const val SURF_API_PAPER_RAW = "dev.slne.surf.api.gradle.paper-raw"
        private const val SURF_API_VELOCITY = "dev.slne.surf.api.gradle.velocity"
        private const val SURF_API_STANDALONE = "dev.slne.surf.api.gradle.standalone"
        private const val MICROSERVICE_PLUGIN = "dev.slne.surf.api.gradle.microservice"
        private const val STANDALONE_MODULE = "surf-api-nats-standalone"

        private val SURF_API_PAPER_PLUGINS = setOf(
            SURF_API_PAPER_PLUGIN,
            SURF_API_PAPER_RAW,
        )

        private val SURF_API_PLUGINS = listOf(
            "dev.slne.surf.api.gradle.core",
            SURF_API_PAPER_PLUGIN,
            SURF_API_PAPER_RAW,
            SURF_API_VELOCITY,
            "dev.slne.surf.api.gradle.minestom",
            "dev.slne.surf.api.gradle.minestom-server",
            SURF_API_STANDALONE,
        )
    }
}
