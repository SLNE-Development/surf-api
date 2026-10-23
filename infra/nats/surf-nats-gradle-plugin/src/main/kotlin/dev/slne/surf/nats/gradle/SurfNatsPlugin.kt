package dev.slne.surf.nats.gradle

import dev.slne.surf.nats.gradle.bridge.SurfApiBridge
import dev.slne.surf.nats.gradle.generator.DefaultTypeMappings
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.TypeMapping
import dev.slne.surf.nats.gradle.tasks.CheckSurfNatsCompatibilityTask
import dev.slne.surf.nats.gradle.tasks.GenerateSurfNatsKotlinTask
import dev.slne.surf.nats.gradle.tasks.ProtoArchiveTransform
import dev.slne.surf.nats.gradle.tasks.ProtoArchives
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Plugin `dev.slne.surf.nats`: generates Kotlin contracts from `.proto` files of Kotlin/JVM projects, packs the
 * protos into the jar and checks them against a published baseline.
 */
class SurfNatsPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        val extension = extensions.create(EXTENSION, SurfNatsExtension::class.java)
        extension.protoSourceDirs.convention(layout.projectDirectory.dir("src/main/proto"))
        extension.generatedVisibility.convention(GeneratedVisibility.PUBLIC)
        extension.compatibilityCheck.convention(true)
        val defaultTypeMappings = objects.listProperty(TypeMapping::class.java).value(DefaultTypeMappings.common)

        registerProtoArchiveTransform()
        val protoPath = resolvableJars(PROTO_PATH_CONFIGURATION, "Jars whose surf-nats protos the .proto sources import")
        val baseline = resolvableJars(BASELINE_CONFIGURATION, "The published artifact the surf-nats contracts must stay compatible with")
        baseline.resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
        baseline.dependencies.addAllLater(
            extension.compatibilityBaseline.map { listOf(dependencies.create(it)) }.orElse(emptyList())
        )

        val generate = tasks.register(GENERATE_TASK, GenerateSurfNatsKotlinTask::class.java) { task ->
            task.group = GROUP
            task.description = "Generates Kotlin contracts from the surf-nats .proto sources."
            task.sourceDirectories.from(extension.protoSourceDirs)
            task.protoPathDirectories.from(extension.protoPath)
            task.protoPathArchives.from(protoPath.protoRoots())
            task.defaultTypeMappings.set(defaultTypeMappings)
            task.typeMappings.set(extension.typeMappings)
            task.visibility.set(extension.generatedVisibility)
            task.outputDir.set(layout.buildDirectory.dir("generated/surf-nats/kotlin"))
        }

        val check = tasks.register(CHECK_TASK, CheckSurfNatsCompatibilityTask::class.java) { task ->
            task.group = LifecycleBasePlugin.VERIFICATION_GROUP
            task.description = "Checks the surf-nats .proto sources against the compatibility baseline."
            task.sourceDirectories.from(extension.protoSourceDirs)
            task.protoPathDirectories.from(extension.protoPath)
            task.protoPathArchives.from(protoPath.protoRoots())
            task.baselineArchives.from(baseline.protoRoots())
            task.baselineJars.from(baseline)
            task.baselineConfigured.set(provider { baseline.allDependencies.isNotEmpty() })
            task.checkEnabled.set(extension.compatibilityCheck)
            task.reportFile.set(layout.buildDirectory.file("reports/surf-nats/compatibility.txt"))
        }

        pluginManager.withPlugin(KOTLIN_JVM) {
            for (configuration in listOf(protoPath, baseline)) {
                configuration.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            }
            extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets.named("main") { sourceSet ->
                sourceSet.kotlin.srcDir(generate.flatMap { it.outputDir })
            }
            pluginManager.apply(KOTLIN_SERIALIZATION)
            packProtos(extension)
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { it.dependsOn(check) }
        }
        pluginManager.withPlugin(KOTLIN_MULTIPLATFORM) {
            throw GradleException("$ID supports Kotlin/JVM projects only; $path applies $KOTLIN_MULTIPLATFORM")
        }

        var paperTypes = false
        for (id in SURF_API_PLUGINS) {
            pluginManager.withPlugin(id) {
                val paper = id in SURF_API_PAPER_PLUGINS
                if (paper && !paperTypes) {
                    paperTypes = true
                    defaultTypeMappings.addAll(DefaultTypeMappings.paper)
                }
                SurfApiBridge.configure(project, paper)
            }
        }
    }

    /**
     * Registers [ProtoArchiveTransform] for jars, zips and directories, selected by [protoRoots].
     */
    private fun Project.registerProtoArchiveTransform() {
        for (type in listOf(ArtifactTypeDefinition.JAR_TYPE, ArtifactTypeDefinition.ZIP_TYPE, ArtifactTypeDefinition.DIRECTORY_TYPE)) {
            dependencies.registerTransform(ProtoArchiveTransform::class.java) { spec ->
                spec.from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, type)
                spec.to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ProtoArchiveTransform.PROTO_ROOT_TYPE)
            }
        }
    }

    /**
     * Returns the proto roots of the resolved artifacts that pack protos, in artifact order.
     */
    private fun Configuration.protoRoots(): FileCollection = incoming.artifactView { view ->
        view.attributes { it.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ProtoArchiveTransform.PROTO_ROOT_TYPE) }
    }.files

    private fun Project.resolvableJars(name: String, description: String): Configuration {
        return configurations.create(name) { configuration ->
            configuration.description = description
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.isTransitive = false
            configuration.attributes { attributes ->
                attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                attributes.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements::class.java, LibraryElements.JAR),
                )
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
            }
        }
    }

    private fun Project.packProtos(extension: SurfNatsExtension) {
        tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources::class.java) { task ->
            task.from(extension.protoSourceDirs) { spec ->
                spec.include("**/*.proto")
                spec.into(ProtoArchives.PREFIX.removeSuffix("/"))
            }
        }
    }

    companion object {
        const val ID = "dev.slne.surf.nats"
        const val EXTENSION = "surfNats"
        const val GENERATE_TASK = "generateSurfNatsKotlin"
        const val CHECK_TASK = "checkSurfNatsCompatibility"
        const val PROTO_PATH_CONFIGURATION = "surfNatsProtoPath"
        const val BASELINE_CONFIGURATION = "surfNatsCompatibilityBaseline"

        private const val GROUP = "surf-nats"
        private const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        private const val KOTLIN_MULTIPLATFORM = "org.jetbrains.kotlin.multiplatform"
        private const val KOTLIN_SERIALIZATION = "org.jetbrains.kotlin.plugin.serialization"

        private val SURF_API_PAPER_PLUGINS = setOf(
            "dev.slne.surf.api.gradle.paper-plugin",
            "dev.slne.surf.api.gradle.paper-raw",
        )

        /**
         * The surf-api plugin ids whose extension is a `CommonSurfExtension`.
         */
        private val SURF_API_PLUGINS = listOf(
            "dev.slne.surf.api.gradle.core",
            "dev.slne.surf.api.gradle.paper-plugin",
            "dev.slne.surf.api.gradle.paper-raw",
            "dev.slne.surf.api.gradle.velocity",
            "dev.slne.surf.api.gradle.minestom",
            "dev.slne.surf.api.gradle.minestom-server",
            "dev.slne.surf.api.gradle.standalone",
        )
    }
}
