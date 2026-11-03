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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Gradle plugin for generating Kotlin contracts from proto files.
 *
 * Supports Kotlin/JVM projects. Applies `dev.slne.weaver` with surf-nats conventions: weaver dependencies are
 * import paths for code generation, published modules publish their protos to weaver with `publish`, and the
 * compatibility check compares against the previous weaver release.
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
        tasks.register(GENERATE_TASK, GenerateSurfNatsKotlinTask::class.java) { task ->
            task.group = GROUP
            task.description = "Generates Kotlin contracts from the surf-nats .proto sources."

            task.sourceDirectories.from(extension.protoSourceDirs)
            task.protoPathDirectories.from(extension.protoPath)
            task.weaverProtoPath.from(weaverImportPath(weaver))

            task.defaultTypeMappings.set(defaultTypeMappings)
            task.typeMappings.set(extension.typeMappings)
            task.visibility.set(extension.generatedVisibility)
            task.outputDir.set(layout.buildDirectory.dir("generated/surf-nats/kotlin"))
        }

    private fun Project.registerBaselineTask(
        extension: SurfNatsExtension,
        weaver: WeaverExtension,
        active: Provider<Boolean>,
    ): TaskProvider<FetchSurfNatsBaselineTask> =
        tasks.register(BASELINE_TASK, FetchSurfNatsBaselineTask::class.java) { task ->
            task.group = GROUP
            task.description = "Downloads the previous weaver release of this module as the compatibility baseline."

            task.url.set(weaver.url)
            task.token.set(weaver.token)
            task.repository.set(weaver.publishing.repository)
            task.artifactGroup.set(weaver.publishing.group)
            task.artifactName.set(weaver.publishing.name)
            task.projectVersion.set(weaver.publishing.version)
            task.requestedVersion.set(extension.compatibilityBaseline)
            task.active.set(extension.compatibilityCheck.zip(active) { enabled, needed -> enabled && needed })
            task.offline.set(gradle.startParameter.isOffline)
            task.outputDir.set(layout.buildDirectory.dir("surf-nats/baseline/protos"))
            task.versionFile.set(layout.buildDirectory.file("surf-nats/baseline/version.txt"))
        }

    private fun Project.registerCompatibilityTask(
        extension: SurfNatsExtension,
        weaver: WeaverExtension,
        baselineTask: TaskProvider<FetchSurfNatsBaselineTask>,
        active: Provider<Boolean>,
    ): TaskProvider<CheckSurfNatsCompatibilityTask> =
        tasks.register(CHECK_TASK, CheckSurfNatsCompatibilityTask::class.java) { task ->
            task.group = LifecycleBasePlugin.VERIFICATION_GROUP
            task.description = "Checks the surf-nats .proto sources against the previous weaver release."

            task.sourceDirectories.from(extension.protoSourceDirs)
            task.protoPathDirectories.from(extension.protoPath)
            task.weaverProtoPath.from(weaverImportPath(weaver))

            task.baselineDirectory.from(baselineTask.flatMap { it.outputDir })
            task.baselineVersionFile.set(baselineTask.flatMap { it.versionFile })
            task.active.set(active)
            task.checkEnabled.set(extension.compatibilityCheck)
            task.reportFile.set(layout.buildDirectory.file("reports/surf-nats/compatibility.txt"))
        }

    private fun Project.configureKotlinPlugins(
        generateTask: TaskProvider<GenerateSurfNatsKotlinTask>,
        compatibilityTask: TaskProvider<CheckSurfNatsCompatibilityTask>,
    ) {
        pluginManager.withPlugin(KOTLIN_JVM) {
            extensions.getByType(KotlinJvmProjectExtension::class.java)
                .sourceSets.named("main") { sourceSet ->
                    sourceSet.kotlin.srcDir(generateTask.flatMap { it.outputDir })
                }

            pluginManager.apply(KOTLIN_SERIALIZATION)

            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
                it.dependsOn(compatibilityTask)
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
    }

    companion object {
        const val ID = "dev.slne.surf.nats"
        const val EXTENSION = "surfNats"
        const val GENERATE_TASK = "generateSurfNatsKotlin"
        const val CHECK_TASK = "checkSurfNatsCompatibility"
        const val STAGE_TASK = "stageSurfNatsWeaverSource"
        const val BASELINE_TASK = "fetchSurfNatsBaseline"

        private const val GROUP = "surf-nats"
        private const val TYPES_COMMON_MODULE = "surf-nats-types-common"
        private const val TYPES_PAPER_MODULE = "surf-nats-types-paper"
        private const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        private const val KOTLIN_MULTIPLATFORM = "org.jetbrains.kotlin.multiplatform"
        private const val KOTLIN_SERIALIZATION = "org.jetbrains.kotlin.plugin.serialization"

        private const val SURF_API_PAPER_PLUGIN = "dev.slne.surf.api.gradle.paper-plugin"
        private const val SURF_API_PAPER_RAW = "dev.slne.surf.api.gradle.paper-raw"
        private const val SURF_API_VELOCITY = "dev.slne.surf.api.gradle.velocity"

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
            "dev.slne.surf.api.gradle.standalone",
        )
    }
}
