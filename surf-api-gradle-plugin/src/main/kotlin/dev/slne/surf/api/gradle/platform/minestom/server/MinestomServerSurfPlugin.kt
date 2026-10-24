package dev.slne.surf.api.gradle.platform.minestom.server

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.generators.GeneratePluginFile
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingConfigurer
import dev.slne.surf.api.gradle.platform.core.AbstractCoreSurfPlugin
import dev.slne.surf.api.gradle.util.minestomSnapshots
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.CommandLineArgumentProvider
import xyz.jpenilla.gremlin.gradle.GremlinExtension
import xyz.jpenilla.gremlin.gradle.WriteDependencySet

/**
 * Builds a runnable surf Minestom server as one executable server jar: the surf api, the
 * requested features and the project's own code.
 *
 * The server jar contains the project's own modules, the bootstrap, the surf modules and the
 * modules declared in the `shaded` configuration. With
 * [MinestomServerSurfExtension.downloadSurfModules] the surf modules are downloaded instead. Every
 * other library is listed in the gremlin dependency set `dependencies.txt`. The bootstrap agent
 * downloads these libraries into `libraries/`, relocates them like the server jar and appends
 * them to the system class path before `main` runs.
 *
 * The `shaded` configuration is extended by `runtimeOnly` and embeds modules that cannot be
 * downloaded, such as those of repositories that need credentials. The whole resolved closure of
 * the dependencies declared in it is embedded, including public modules they depend on. A module
 * of that closure is never downloaded, regardless of other paths to it.
 *
 * Manifest attributes written to the jar:
 * - `Launcher-Agent-Class`: the bootstrap agent.
 * - `Surf-Delegate-Agent-Classes`: delegate agents the bootstrap agent calls after the libraries
 *   are installed. `SurfMixinAgent` is always first so its transformer exists before any other
 *   agent loads a Minestom class.
 * - `Surf-Mixin-Configs`: mixin configurations of surf, the features and the project.
 * - `Surf-Plugin-Dependencies`: plugin IDs from `pluginDependencies`, which the bootstrap puts on
 *   the system class path.
 *
 * The server jar applies the same relocations as the `dev.slne.surf.api.gradle.minestom` Gradle
 * plugin. Plugins built with that plugin therefore refer to the packages the server actually
 * contains, whether they are dropped into `plugins/` or shaded in through a project dependency.
 */
internal class MinestomServerSurfPlugin :
    AbstractCoreSurfPlugin<MinestomServerSurfExtension>("minestomServer", SurfApiPlatform.MINESTOM_SERVER) {

    init {
        // The core relocations (mojang, adventure nbt, configurate) come from the base class
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
        "me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
    }

    override val extensionClass = MinestomServerSurfExtension::class.java

    override fun Project.applyRepositories0() {
        repositories { minestomSnapshots() }
    }

    override fun Project.applyPlugins0() {
        applyPlugin("org.gradle.application")
        applyPlugin("xyz.jpenilla.gremlin-gradle")
    }

    override fun Project.configure0() {
        val shaded = configurations.create(SHADED) {
            description = "Dependencies embedded in the server jar instead of being downloaded, " +
                    "for repositories that need credentials"
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        configurations.named("runtimeOnly") { extendsFrom(shaded) }

        dependencies {
            "implementation"(Constants.MINESTOM)
            "runtimeOnly"("org.apache.logging.log4j:log4j-core:${Constants.LOG4J_VERSION}")
            "runtimeOnly"("org.apache.logging.log4j:log4j-slf4j2-impl:${Constants.LOG4J_VERSION}")
        }

        tasks.named<ShadowJar>("shadowJar") {
            // log4j-core and its add-ons each ship a Log4j plugin cache; all must reach the
            // transformer to be merged rather than dropped as duplicates
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            mergeServiceFiles()
            transform(Log4j2PluginsCacheFileTransformer::class.java)
            manifest.attributes["Multi-Release"] = "true"
        }

        extensions.configure<GremlinExtension> {
            // The bootstrap module brings the gremlin runtime, which is shaded instead of downloaded
            defaultGremlinRuntimeDependency.set(false)
            // gremlin's default ASM cannot read the class files of current Java versions
            defaultJarRelocatorDependencies.set(false)
        }
        dependencies {
            add(JAR_RELOCATOR_RUNTIME, "me.lucko:jar-relocator:1.7")
            add(JAR_RELOCATOR_RUNTIME, "org.ow2.asm:asm:${Constants.ASM_VERSION}")
            add(JAR_RELOCATOR_RUNTIME, "org.ow2.asm:asm-commons:${Constants.ASM_VERSION}")
        }
    }

    override fun Project.afterEvaluated1(extension: MinestomServerSurfExtension) {
        val features = extension.features.get()

        dependencies {
            features.forEach { feature ->
                add("implementation", "dev.slne.surf.api:${feature.module}:${Constants.SURF_API_VERSION}")
            }
        }

        val mainClass = extension.mainClass.orNull
        if (mainClass == null) {
            logger.warn("No main class set for the Minestom server ${project.path}; call mainClass(...) in surfMinestomServerApi { }")
        } else {
            configure<JavaApplication> { this.mainClass.set(mainClass) }
        }

        dependencies {
            add("implementation", "dev.slne.surf.api:$BOOTSTRAP_MODULE:${Constants.SURF_API_VERSION}")
        }

        // Mixin goes first: its transformer must be installed before another agent loads a Minestom class
        val agents = listOf(MIXIN_AGENT) + features.mapNotNull(MinestomServerFeature::agentClass)
        val mixinConfigs = listOf(MIXIN_CONFIG) +
                features.mapNotNull(MinestomServerFeature::mixinConfig) +
                extension.mixinConfigs.get()
        val pluginDependencies = extension.pluginDependencies
            .filter { it.enabled.get() }
            .map { if (it.optional.get()) "${it.name}?" else it.name }
        tasks.named<ShadowJar>("shadowJar") {
            if (mainClass != null) manifest.attributes["Main-Class"] = mainClass
            manifest.attributes["Launcher-Agent-Class"] = BOOTSTRAP_AGENT
            manifest.attributes[DELEGATE_AGENTS_ATTRIBUTE] = agents.joinToString(",")
            manifest.attributes[MIXIN_CONFIGS_ATTRIBUTE] = mixinConfigs.distinct().joinToString(",")
            if (pluginDependencies.isNotEmpty()) {
                manifest.attributes[PLUGIN_DEPENDENCIES_ATTRIBUTE] = pluginDependencies.joinToString(",")
            }
        }

        if (extension.downloadLibraries.get()) {
            configureLibraryDownload(extension)
        } else {
            if (extension.downloadSurfModules.get()) {
                logger.warn("downloadSurfModules has no effect on ${project.path} while downloadLibraries is disabled")
            }
            tasks.named<ProcessResources>("processResources") { exclude(DEPENDENCIES_FILE) }
            configureShadedAsmVersion()
        }

        registerRunServer(extension)
        registerDefaultLoggingConfiguration(console = MinestomServerFeature.CONSOLE in features)
    }

    /**
     * Generates a default `log4j2.xml` (console and rolling file) unless the project ships its own.
     * Without a configuration, Log4j logs errors only. When the console feature is installed, the
     * console appender is the terminal console.
     */
    private fun Project.registerDefaultLoggingConfiguration(console: Boolean) {
        val ownConfiguration = layout.projectDirectory.file("src/main/resources/$LOG4J_CONFIG").asFile
        if (ownConfiguration.exists()) return

        val appender = if (console) {
            """<TerminalConsole name="Console">
              |      <PatternLayout pattern="%highlightError{[%d{HH:mm:ss} %level]: [%logger{1}] %msg%n%xEx{full}}"/>
              |    </TerminalConsole>""".trimMargin()
        } else {
            """<Console name="Console" target="SYSTEM_OUT">
              |      <PatternLayout pattern="[%d{HH:mm:ss} %level]: [%logger{1}] %msg%n%xEx{full}"/>
              |    </Console>""".trimMargin()
        }

        val configuration = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<!-- Generated by dev.slne.surf.api.gradle.minestom-server; add src/main/resources/$LOG4J_CONFIG to replace it -->
            |<Configuration status="WARN" shutdownHook="disable">
            |  <Appenders>
            |    $appender
            |    <RollingRandomAccessFile name="File" fileName="logs/latest.log" filePattern="logs/%d{yyyy-MM-dd}-%i.log.gz">
            |      <PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level]: [%logger] %msg%n%xEx{full}"/>
            |      <Policies>
            |        <OnStartupTriggeringPolicy/>
            |        <TimeBasedTriggeringPolicy/>
            |      </Policies>
            |      <DefaultRolloverStrategy max="30"/>
            |    </RollingRandomAccessFile>
            |  </Appenders>
            |  <Loggers>
            |    <Root level="info">
            |      <AppenderRef ref="Console"/>
            |      <AppenderRef ref="File"/>
            |    </Root>
            |  </Loggers>
            |</Configuration>
            |""".trimMargin()

        val outputDirectory = layout.buildDirectory.dir("generated/surf-api/minestom-server-logging")
        val generate = tasks.register<GeneratePluginFile>("generateDefaultLoggingConfiguration") {
            group = "surf-api"
            fileName.set(LOG4J_CONFIG)
            outputDir.set(outputDirectory)
            pluginFileJson.set(configuration)
        }

        extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
            resources.srcDir(generate)
        }
    }

    /**
     * Writes every library that is not shaded, with the server jar's relocations, to
     * `dependencies.txt`, the file the bootstrap downloads from. The [ShadingRules] decide which
     * modules stay in the server jar.
     */
    private fun Project.configureLibraryDownload(extension: MinestomServerSurfExtension) {
        val runtimeClasspath = configurations.getByName("runtimeClasspath")
        val shaded = configurations.named(SHADED) {
            // Selects the same versions as the runtime graph and resolves the same variants
            shouldResolveConsistentlyWith(runtimeClasspath)
            attributes {
                runtimeClasspath.attributes.keySet().forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    attribute(key as Attribute<Any>, runtimeClasspath.attributes.getAttribute(key)!!)
                }
            }
        }
        val rules = ShadingRules(
            downloadSurfModules = extension.downloadSurfModules.get(),
            embedded = shaded.map { configuration ->
                configuration.incoming.resolutionResult.allComponents
                    .mapNotNull { component ->
                        (component.id as? ModuleComponentIdentifier)?.let { it.group to it.module }
                    }
                    .toSet()
            },
        )

        configurations.named(RUNTIME_DOWNLOAD) {
            extendsFrom(configurations.getByName("implementation"), configurations.getByName("runtimeOnly"))
            shouldResolveConsistentlyWith(configurations.getByName("runtimeClasspath"))
        }

        tasks.named<WriteDependencySet>("writeDependencies") {
            // Filtered per component, so that the libraries of shaded modules are still downloaded
            dependencies.setFrom(configurations.named(RUNTIME_DOWNLOAD).map { configuration ->
                // Resolves `shaded` before the artifact view, not during its resolution
                rules.embeddedModules
                configuration.incoming.artifactView {
                    componentFilter { id -> id !is ModuleComponentIdentifier || !rules.isShaded(id.group, id.module) }
                }.artifacts
            })
            forEachRelocation { from, to, excludes ->
                relocate(from, to) { this.excludes.set(excludes) }
            }
            if (extension.withSurfRedis.get()) {
                extension.surfRedisRelocation.orNull?.let { relocate("dev.slne.surf.redis", it) }
            }
            if (extension.withSurfDatabaseR2dbc.get()) {
                extension.surfDatabaseR2dbcRelocation.orNull?.let { relocate("dev.slne.surf.database", it) }
            }
        }

        tasks.named<ShadowJar>("shadowJar") {
            dependencies {
                exclude { dependency -> !rules.isShaded(dependency) }
            }
        }
    }

    /**
     * Writes the ASM version into the `org/objectweb/asm/` manifest section. Mixin reads the
     * version there, and the server jar lacks that section once ASM is shaded in. Applied only when
     * the libraries are shaded instead of downloaded.
     */
    private fun Project.configureShadedAsmVersion() {
        val asmVersion = configurations.named("runtimeClasspath")
            .flatMap { it.incoming.artifacts.resolvedArtifacts }
            .map { artifacts ->
                artifacts.asSequence()
                    .map { it.id.componentIdentifier }
                    .filterIsInstance<ModuleComponentIdentifier>()
                    .firstOrNull { it.group == "org.ow2.asm" && it.module == "asm" }
                    ?.version
                    .orEmpty()
            }
        tasks.named<ShadowJar>("shadowJar") {
            doFirst {
                val version = asmVersion.get()
                if (version.isNotEmpty()) {
                    manifest.attributes(mapOf("Implementation-Version" to version), "org/objectweb/asm/")
                }
            }
        }
    }

    /**
     * Decides which modules stay in the server jar when the libraries are downloaded.
     *
     * A module is shaded when it is one of the [BOOTSTRAP_MODULES], a project dependency, part of
     * the `shaded` closure or, unless [downloadSurfModules] is set, a surf module. Surf modules
     * exclude the third-party forks under `dev.slne.forks`. Every component therefore has exactly
     * one destination: the server jar or `dependencies.txt`.
     *
     * The `shaded` closure is resolved on first use, at execution time, and then kept.
     *
     * @param embedded `group` to `name` of every module in the resolution result of `shaded`
     */
    private class ShadingRules(
        private val downloadSurfModules: Boolean,
        embedded: Provider<Set<Pair<String, String>>>,
    ) {
        val embeddedModules: Set<Pair<String, String>> by lazy { embedded.get() }

        fun isShaded(group: String, module: String): Boolean {
            val id = group to module
            return id in BOOTSTRAP_MODULES ||
                    id in embeddedModules ||
                    (!downloadSurfModules && (group == "dev.slne.surf" || group.startsWith("dev.slne.surf.")))
        }

        fun isShaded(dependency: ResolvedDependency) =
            isShaded(dependency.moduleGroup, dependency.moduleName) ||
                    dependency.moduleArtifacts.any { it.id.componentIdentifier is ProjectComponentIdentifier }
    }

    /**
     * Registers `runServer`, which runs the server jar with `java -jar` so that the manifest's
     * agents apply.
     */
    private fun Project.registerRunServer(extension: MinestomServerSurfExtension) {
        val shadowJar = tasks.named<ShadowJar>("shadowJar")
        val runDirectory = layout.projectDirectory.dir(extension.runDirectory.get()).asFile
        val launcher = extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(Constants.JAVA_VERSION))
        }

        tasks.register<Exec>("runServer") {
            group = "surf-api"
            description = "Builds the server jar and runs it in ${extension.runDirectory.get()}/"
            dependsOn(shadowJar)

            workingDir = runDirectory
            standardInput = System.`in`
            doFirst {
                runDirectory.mkdirs()
                executable = launcher.get().executablePath.asFile.absolutePath
            }
            argumentProviders += CommandLineArgumentProvider {
                listOf("-jar", shadowJar.get().archiveFile.get().asFile.absolutePath)
            }
        }
    }

    private companion object {
        const val LOG4J_CONFIG = "log4j2.xml"

        const val RUNTIME_DOWNLOAD = "runtimeDownload"
        const val SHADED = "shaded"
        const val JAR_RELOCATOR_RUNTIME = "jarRelocatorRuntime"
        const val DEPENDENCIES_FILE = "dependencies.txt"
        const val BOOTSTRAP_MODULE = "surf-api-minestom-server-bootstrap"
        const val BOOTSTRAP_AGENT = "dev.slne.surf.api.minestom.server.bootstrap.SurfMinestomBootstrap"
        const val DELEGATE_AGENTS_ATTRIBUTE = "Surf-Delegate-Agent-Classes"
        const val PLUGIN_DEPENDENCIES_ATTRIBUTE = "Surf-Plugin-Dependencies"
        const val MIXIN_AGENT = "dev.slne.surf.api.minestom.server.impl.mixin.SurfMixinAgent"
        const val MIXIN_CONFIGS_ATTRIBUTE = "Surf-Mixin-Configs"
        const val MIXIN_CONFIG = "mixins.surf-api-minestom-server.json"

        /**
         * What the bootstrap needs before anything is downloaded, independent of
         * [MinestomServerSurfExtension.downloadSurfModules].
         */
        val BOOTSTRAP_MODULES = setOf(
            "dev.slne.surf.api" to BOOTSTRAP_MODULE,
            "xyz.jpenilla" to "gremlin-runtime",
            "org.jspecify" to "jspecify",
        )
    }

    override fun Project.platformTestDependencies(extension: MinestomServerSurfExtension) {
        val testing = extension.testing
        if (!testing.minestomTesting.get()) return
        dependencies {
            add(
                SurfTestingConfigurer.TEST_IMPLEMENTATION,
                "net.minestom:testing:${testing.minestomTestingVersion.get()}"
            )
        }
    }
}
