package dev.slne.surf.api.gradle.pluginclass

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import java.io.File

private const val TASK_GROUP = "surf-api"

/**
 * The tasks that give a plugin its generated main class and verify it: the base package, the
 * generated classes, the superclass check and the bundled Spring check.
 *
 * Every value is a provider, so nothing reads the sources or the extension while the build is
 * configured, and no task action or `onlyIf` captures the project.
 */
internal class SurfPluginClassTasks(
    private val project: Project,
    private val extension: CoreSurfExtension,
    private val platform: SurfApiPlatform,
) {
    private val classesToGenerate = project.objects.listProperty<GeneratedPluginClass>()
    private val librariesLoaderEnabled = project.objects.property<Boolean>().convention(false)
    private val namedScanClass = project.objects.property<String>()

    private val computeBasePackage: TaskProvider<ComputeBasePackageTask> =
        project.tasks.register<ComputeBasePackageTask>("computeSurfBasePackage") {
            group = TASK_GROUP
            description = "Resolves the package of the generated plugin classes and checks the package the plugin scans."
            sourceFiles.from(ownSourceFiles())
            explicitBasePackage.set(extension.basePackage)
            generatesCode.set(
                classesToGenerate.map { it.isNotEmpty() }.zip(librariesLoaderEnabled) { classes, loader -> classes || loader }
            )
            scannedClass.set(namedScanClass)
            this.platform.set(this@SurfPluginClassTasks.platform)
            outputFile.set(project.layout.buildDirectory.file("generated/surf-api/base-package/base-package.txt"))
        }

    /**
     * The package of the generated classes and the libraries loader, produced by
     * [computeBasePackage]. Empty when nothing generated needs a package.
     */
    val basePackage: Provider<String> =
        computeBasePackage.flatMap { it.outputFile }.map { it.asFile.readText() }

    /**
     * Makes [task] run after the base package is computed. Needed for a task that reads the
     * package through a nested bean, because Gradle does not follow the providers inside one.
     */
    fun dependOnBasePackage(task: Task) {
        task.dependsOn(computeBasePackage)
    }

    /**
     * The fully qualified name of the generated [kind] in the base package, absent when
     * `generatePluginClass` is off.
     */
    fun generatedClassName(kind: GeneratedPluginClass): Provider<String> {
        val generated = basePackage.map(kind::qualifiedName)
        val none = project.objects.property<String>()
        return extension.generatePluginClass.flatMap { enabled -> if (enabled) generated else none }
    }

    /**
     * Declares [namedClass], the class the build script names and the runtime scans from, so that
     * its package is checked against the sources.
     */
    fun scanFrom(namedClass: Provider<String>) {
        namedScanClass.set(namedClass)
    }

    /**
     * Declares whether the build generates the libraries loader, which needs the base package.
     */
    fun generatesLibrariesLoader(enabled: Provider<Boolean>) {
        librariesLoaderEnabled.set(enabled)
    }

    /**
     * Generates [classes] into `build/generated/surf-api/[directory]` and compiles them with the plugin.
     *
     * Generates nothing when `generatePluginClass` is off.
     */
    fun generateClasses(directory: String, classes: Provider<List<GeneratedPluginClass>>) {
        classesToGenerate.set(extension.generatePluginClass.zip(classes) { enabled, kinds ->
            if (enabled) kinds else emptyList()
        })

        val generate = project.tasks.register<GeneratePluginClassesTask>("generateSurfPluginClasses") {
            group = TASK_GROUP
            description = "Generates the plugin classes the build script does not name."
            this.basePackage.set(this@SurfPluginClassTasks.basePackage)
            this.classes.set(classesToGenerate)
            outputDirectory.set(project.layout.buildDirectory.dir("generated/surf-api/$directory"))
        }

        project.plugins.withType<JavaPlugin> {
            project.mainSourceSet().java.srcDir(generate.flatMap { it.outputDirectory })
        }
    }

    /**
     * Fails `check` and the jar tasks when a class in [expectedBases] does not extend its surf base class.
     */
    fun verifySuperclasses(expectedBases: Provider<Map<String, String>>) {
        val verifyPluginClasses = extension.verifyPluginClasses
        val check = project.tasks.register<CheckSurfPluginClassesTask>("checkSurfPluginClasses") {
            group = "verification"
            description = "Checks that the plugin classes named in the build script extend the surf base classes."
            this.expectedBases.set(expectedBases)
            classDirectories.from(project.mainSourceSet().output.classesDirs)
            compileClasspath.from(project.mainSourceSet().compileClasspath)
            passMarker.set(project.layout.buildDirectory.file("surf-api/verification/$name.passed"))
            onlyIf("verifyPluginClasses is enabled and a class is named") {
                verifyPluginClasses.get() && expectedBases.get().isNotEmpty()
            }
        }
        runBeforePackaging(check)
    }

    /**
     * Fails `check` and the shadow jar tasks when the shadow jar bundles Spring.
     *
     * The plain `jar` task does not wait for the check: it bundles no dependencies, and the check
     * needs the artifacts of every bundled in-build project.
     *
     * Does nothing when surf-api is declared outside `compileOnly`: surf-api then travels with the
     * plugin and so does its Spring. The check reads the coordinates and the contents of the artifacts
     * of the configurations the shadow jar bundles, so includes and excludes of the shadow jar itself
     * are not considered.
     */
    fun forbidBundledSpring() {
        project.plugins.withType<ShadowJavaPlugin> {
            val allowBundledSpring = extension.allowBundledSpring
            val surfApiIsProvided = extension.surfApiScope.orElse(platform.scope)
                .map { it == COMPILE_ONLY || it == "compileOnlyApi" }
            val shadowJar = project.tasks.named("shadowJar", ShadowJar::class.java)

            val check = project.tasks.register<CheckNoBundledSpringTask>("checkNoBundledSpring") {
                group = "verification"
                description = "Checks that the plugin jar does not bundle Spring."
                val bundledConfigurations = shadowJar.flatMap { it.configurations }
                bundledModules.set(bundledConfigurations.map { it.bundledModules() })
                bundledArtifacts.from(bundledConfigurations)
                passMarker.set(project.layout.buildDirectory.file("surf-api/verification/$name.passed"))
                onlyIf("allowBundledSpring is off and surf-api is compileOnly") {
                    !allowBundledSpring.get() && surfApiIsProvided.get()
                }
            }
            runBeforeShadowJar(check)
        }
    }

    private fun runBeforePackaging(check: TaskProvider<*>) {
        runWithCheck(check)
        project.tasks.withType<Jar>().configureEach {
            if (name == "jar" || this is ShadowJar) dependsOn(check)
        }
    }

    private fun runBeforeShadowJar(check: TaskProvider<*>) {
        runWithCheck(check)
        project.tasks.withType<ShadowJar>().configureEach { dependsOn(check) }
    }

    private fun runWithCheck(task: TaskProvider<*>) {
        project.tasks.named("check") { dependsOn(task) }
    }

    private fun ownSourceFiles() = project.files(project.provider {
        val buildDirectory = project.layout.buildDirectory.get().asFile.toPath()
        val kotlinDirectories = project.extensions.getByType<KotlinJvmProjectExtension>()
            .sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).kotlin.srcDirs
        val directories: Set<File> = project.mainSourceSet().java.srcDirs + kotlinDirectories

        directories
            .filter { !it.toPath().startsWith(buildDirectory) }
            .map { directory ->
                project.fileTree(directory) {
                    include("**/*.kt", "**/*.java")
                    exclude("**/module-info.java")
                }
            }
    })

    private fun Project.mainSourceSet(): SourceSet =
        extensions.getByType<SourceSetContainer>().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
}

/**
 * Returns the modules, as `group:name:version`, of the artifacts these configurations resolve to.
 * A module that only contributes constraints, such as a BOM, has no artifact and is left out.
 */
private fun Collection<Configuration>.bundledModules(): Set<String> =
    flatMap { it.incoming.artifacts.artifacts }
        .mapNotNull { it.id.componentIdentifier as? ModuleComponentIdentifier }
        .mapTo(linkedSetOf()) { "${it.group}:${it.module}:${it.version}" }
