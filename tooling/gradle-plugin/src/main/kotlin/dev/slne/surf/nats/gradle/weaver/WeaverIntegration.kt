package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.SurfNatsExtension
import dev.slne.surf.nats.gradle.SurfNatsPluginVersion
import dev.slne.weaver.gradle.FetchProtosTask
import dev.slne.weaver.gradle.PublishProtosTask
import dev.slne.weaver.gradle.WeaverExtension
import dev.slne.weaver.gradle.WeaverPlugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import java.util.concurrent.Callable

/**
 * Applies weaver to a surf-nats project and wires it into the surf-nats tasks.
 *
 * A project is a *published module* when `maven-publish` is applied and at least one publishing repository is
 * configured ([isPublishedModule]). Only published modules publish their protos with `publish`, validate their
 * weaver coordinates and run the compatibility check against weaver by default.
 */
internal object WeaverIntegration {

    const val CONFIGURATION = WeaverPlugin.CONFIGURATION
    const val FETCH_TASK = "fetchProtos"
    const val PUBLISH_TASK = "publishProtos"
    const val DEFAULT_URL = "https://weaver.slne.dev"
    const val DEFAULT_REPOSITORY = "proto-releases-public"

    private const val MAVEN_PUBLISH = "maven-publish"
    private const val PROTO_PATTERN = "**/*.proto"

    /**
     * Applies `dev.slne.weaver` and sets the surf-nats conventions on its extension.
     *
     * The `WEAVER_URL` environment variable takes precedence over the default URL.
     */
    fun Project.applyWeaver(): WeaverExtension {
        pluginManager.apply(WeaverPlugin::class.java)

        val weaver = extensions.getByType(WeaverExtension::class.java)
        weaver.url.convention(providers.environmentVariable("WEAVER_URL").orElse(DEFAULT_URL))
        weaver.repositories.convention(listOf(DEFAULT_REPOSITORY))
        weaver.publishing.repository.convention(DEFAULT_REPOSITORY)
        weaver.publishing.group.convention(provider { explicitGroup() })
        weaver.publishing.name.convention(defaultName())

        return weaver
    }

    /**
     * Returns `rootProject.name` for published modules and nothing otherwise.
     *
     * Unpublished modules have no coordinate, so their weaver manifests do not collide when one project depends on
     * several of them through `weaver(project(...))`.
     */
    private fun Project.defaultName(): Provider<String> {
        val rootName = rootProject.name
        return publishedModule().map { published -> if (published) rootName else null }
    }

    /**
     * Returns the artifact name the compatibility baseline is looked up under: `weaver.publishing.name`, or
     * `rootProject.name` for unpublished modules with an explicit `compatibilityBaseline`.
     */
    fun Project.baselineName(weaver: WeaverExtension): Provider<String> =
        weaver.publishing.name.orElse(rootProject.name)

    /**
     * Returns `project.group`, or `null` when it is blank or the default Gradle derives for subprojects
     * (`rootProject.name` plus the parent path).
     */
    private fun Project.explicitGroup(): String? {
        val group = group.toString()
        val derived = when (val parent = parent) {
            null -> ""
            rootProject -> rootProject.name
            else -> rootProject.name + parent.path.replace(':', '.')
        }
        return group.takeUnless { it.isBlank() || it == derived }
    }

    /**
     * Registers the task that copies the `.proto` files of [SurfNatsExtension.protoSourceDirs] to the directory
     * weaver publishes, and fixes `weaver.publishing.source` to it.
     */
    fun Project.stageWeaverSource(
        extension: SurfNatsExtension,
        weaver: WeaverExtension,
        taskName: String,
        group: String,
    ): TaskProvider<Sync> {
        val stage = tasks.register(taskName, Sync::class.java) {
            this.group = group
            this.description = "Copies the surf-nats .proto sources to the directory weaver publishes."
            this.from(extension.protoSourceDirs) { include(PROTO_PATTERN) }
            this.includeEmptyDirs = false
            this.into(layout.buildDirectory.dir("surf-nats/weaver-source"))
        }

        weaver.publishing.source.set(layout.dir(stage.map { it.destinationDir }))
        weaver.publishing.source.disallowChanges()

        return stage
    }

    /**
     * Returns the weaver proto root as a lazily evaluated file-collection element.
     *
     * Resolves to `weaver.protosDir`, which carries the `fetchProtos` dependency, only while the `weaver`
     * configuration declares dependencies, and otherwise to nothing, so builds without weaver dependencies stay
     * offline.
     */
    fun Project.weaverImportPath(weaver: WeaverExtension): Callable<Any> = Callable {
        if (configurations.getByName(CONFIGURATION).dependencies.isEmpty()) emptyList<Any>() else weaver.protosDir
    }

    /**
     * Fails `fetchProtos` when Gradle runs offline and remote weaver dependencies must be resolved.
     */
    fun Project.guardOfflineFetch() {
        val offline = gradle.startParameter.isOffline
        tasks.named(FETCH_TASK, FetchProtosTask::class.java) {
            this.doFirst(OfflineFetchGuard(offline))
        }
    }

    /**
     * Adds the surf-nats type artifacts ([typeModules], at this plugin's version) to the manifest of `publishProtos`.
     *
     * Only the publish manifest gets them, so that the registry can compile imports of the bundled protos. The
     * `weaver` configuration stays unchanged, because every dependency there would make `fetchProtos` need the
     * network. A module the user declares in `weaver` takes precedence over the automatic one.
     */
    fun Project.addTypeDependenciesToPublish(typeModules: ListProperty<String>) {
        val declared = configurations.named(CONFIGURATION).map { configuration ->
            configuration.dependencies.withType(ExternalModuleDependency::class.java).map {
                WeaverCoordinates.external(it.group, it.name, it.version)
            }
        }
        val automatic = typeModules.map { modules ->
            modules.map { "${SurfNatsPluginVersion.GROUP}:$it:${SurfNatsPluginVersion.VERSION}" }
        }

        tasks.named(PUBLISH_TASK, PublishProtosTask::class.java) {
            this.dependencies.set(declared.zip(automatic) { user, types ->
                val userKeys = user.map { it.substringBeforeLast(':') }.toSet()
                user + types.filter { it.substringBeforeLast(':') !in userKeys }
            })
        }
    }

    /**
     * Wires `publishProtos` into publishing.
     *
     * `publishProtos` runs after code generation and the compatibility check, and only when there are staged
     * protos, and after the `publishProtos` of every `weaver(project(...))` dependency, whose coordinate the registry
     * must already know. For published modules `publish` depends on it, and every Maven publication runs after it, so
     * that a rejected upload fails before the Maven release.
     */
    fun Project.wirePublishProtos(vararg prerequisites: TaskProvider<out Task>) {
        val localPublishTasks = configurations.named(CONFIGURATION).map { configuration ->
            configuration.dependencies.withType(ProjectDependency::class.java).map { publishTaskPath(it.path) }
        }
        val publishProtos = tasks.named(PUBLISH_TASK, PublishProtosTask::class.java) {
            this.dependsOn(*prerequisites)
            this.mustRunAfter(localPublishTasks)
            this.onlyIf("weaver.publishing.source contains .proto files", HasStagedProtos())
        }

        pluginManager.withPlugin(MAVEN_PUBLISH) {
            tasks.named("publish") {
                this.dependsOn(provider { if (isPublishedModule()) listOf(publishProtos) else emptyList() })
            }
            tasks.withType(PublishToMavenRepository::class.java).configureEach {
                this.mustRunAfter(publishProtos)
            }
        }
    }

    /**
     * Validates the weaver coordinates of published modules with proto sources once all projects are evaluated.
     *
     * Fails on coordinates the registry would reject, and when two projects of the build publish the same
     * `group:name`. Runs after every `afterEvaluate`, so that publishing repositories added there count.
     */
    fun Project.validateWeaverCoordinates(extension: SurfNatsExtension, weaver: WeaverExtension) {
        gradle.projectsEvaluated {
            if (!isPublishedModule() || !hasProtoSources(extension)) return@projectsEvaluated

            val publishing = weaver.publishing
            val group = publishing.group.orNull
            val name = publishing.name.orNull
            val problems = WeaverCoordinates.problems(
                group = group,
                name = name,
                version = publishing.version.orNull,
                repository = publishing.repository.orNull,
                repositories = weaver.repositories.get(),
            )
            if (problems.isNotEmpty()) {
                throw GradleException(
                    "surf-nats: invalid weaver configuration in $path:\n" + problems.joinToString("\n") { "  - $it" }
                )
            }

            val coordinate = "$group:$name"
            val owner = WeaverCoordinateRegistry.claim(gradle, coordinate, path) ?: return@projectsEvaluated
            throw GradleException(
                "surf-nats: projects $owner and $path both publish the weaver artifact $coordinate. " +
                        "Set a distinct weaver { publishing { name = \"...\" } } in each (it defaults to rootProject.name)."
            )
        }
    }

    /**
     * Returns whether `maven-publish` is applied and at least one publishing repository is configured.
     */
    fun Project.isPublishedModule(): Boolean =
        pluginManager.hasPlugin(MAVEN_PUBLISH) &&
                extensions.getByType(PublishingExtension::class.java).repositories.isNotEmpty()

    /**
     * Returns [isPublishedModule] as a provider evaluated when it is queried.
     */
    fun Project.publishedModule(): Provider<Boolean> = provider { isPublishedModule() }

    private fun publishTaskPath(projectPath: String): String =
        if (projectPath == ":") ":$PUBLISH_TASK" else "$projectPath:$PUBLISH_TASK"

    private fun hasProtoSources(extension: SurfNatsExtension): Boolean =
        !extension.protoSourceDirs.asFileTree.matching { include(PROTO_PATTERN) }.isEmpty

    private class HasStagedProtos : Spec<Task> {
        override fun isSatisfiedBy(element: Task): Boolean {
            val source = (element as PublishProtosTask).sourceDir.get().asFile
            return source.isDirectory && source.walkTopDown().any { it.isFile && it.extension == "proto" }
        }
    }
}
