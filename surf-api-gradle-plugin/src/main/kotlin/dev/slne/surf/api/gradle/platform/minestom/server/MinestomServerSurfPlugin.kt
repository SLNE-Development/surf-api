package dev.slne.surf.api.gradle.platform.minestom.server

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.generators.GeneratePluginFile
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingConfigurer
import dev.slne.surf.api.gradle.platform.core.AbstractCoreSurfPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.CommandLineArgumentProvider
import xyz.jpenilla.gremlin.gradle.GremlinExtension
import xyz.jpenilla.gremlin.gradle.WriteDependencySet

/**
 * A runnable surf Minestom server: the surf api server, Minestom, the features asked for and the
 * project's own code as one executable jar.
 *
 * Only the project's own modules, the surf modules and the bootstrap are shaded. Every other
 * library is listed in a gremlin `dependencies.txt` instead, which the bootstrap agent
 * downloads into `libraries/`, relocates like the shadow jar and appends to the class path
 * before `main` runs.
 *
 * The jar applies the same relocations as `dev.slne.surf.api.gradle.minestom`, so plugins built
 * with it, whether dropped into `plugins/` or shaded into the server through a project
 * dependency, refer to the packages the server actually contains.
 */
internal class MinestomServerSurfPlugin :
    AbstractCoreSurfPlugin<MinestomServerSurfExtension>("minestomServer", SurfApiPlatform.MINESTOM_SERVER) {

    init {
        // The core relocations (mojang, adventure nbt, configurate) come from the base class
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
        "me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
    }

    override val extensionClass = MinestomServerSurfExtension::class.java

    override fun Project.applyPlugins0() {
        applyPlugin("org.gradle.application")
        applyPlugin("xyz.jpenilla.gremlin-gradle")
    }

    override fun Project.configure0() {
        dependencies {
            add("implementation", Constants.MINESTOM)
            add("runtimeOnly", "org.apache.logging.log4j:log4j-core:${Constants.LOG4J_VERSION}")
            add("runtimeOnly", "org.apache.logging.log4j:log4j-slf4j2-impl:${Constants.LOG4J_VERSION}")
        }

        tasks.named<ShadowJar>("shadowJar") {
            // log4j-core and its add-ons each ship a plugin cache, which have to reach the
            // transformer to be merged instead of being dropped as duplicates
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

        val agents = features.mapNotNull(MinestomServerFeature::agentClass)
        val downloadLibraries = extension.downloadLibraries.get()
        tasks.named<ShadowJar>("shadowJar") {
            if (mainClass != null) manifest.attributes["Main-Class"] = mainClass
            if (downloadLibraries) {
                manifest.attributes["Launcher-Agent-Class"] = BOOTSTRAP_AGENT
                if (agents.isNotEmpty()) manifest.attributes[DELEGATE_AGENTS_ATTRIBUTE] = agents.joinToString(",")
            } else if (agents.isNotEmpty()) {
                manifest.attributes["Launcher-Agent-Class"] = agents.first()
            }
        }

        if (downloadLibraries) {
            configureLibraryDownload(extension)
        } else {
            tasks.named<ProcessResources>("processResources") { exclude(DEPENDENCIES_FILE) }
        }

        registerRunServer(extension)
        registerDefaultLoggingConfiguration(console = MinestomServerFeature.CONSOLE in features)
    }

    /**
     * Without a configuration, Log4j only logs errors. Unless the project brings a `log4j2.xml`
     * of its own, one is generated that logs to the console, through the terminal console when
     * the console feature is installed.
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
     * Lists every third-party library in the `dependencies.txt` the bootstrap downloads them
     * from, with the shadow jar's relocations, and leaves the project's own modules, the surf
     * modules and the bootstrap in the server jar.
     */
    private fun Project.configureLibraryDownload(extension: MinestomServerSurfExtension) {
        dependencies {
            add("implementation", "dev.slne.surf.api:$BOOTSTRAP_MODULE:${Constants.SURF_API_VERSION}")
        }

        configurations.named(RUNTIME_DOWNLOAD) {
            extendsFrom(configurations.getByName("implementation"), configurations.getByName("runtimeOnly"))
            shouldResolveConsistentlyWith(configurations.getByName("runtimeClasspath"))
        }

        tasks.named<WriteDependencySet>("writeDependencies") {
            // Filtered per component, so that the libraries of shaded modules are still downloaded
            dependencies.setFrom(configurations.named(RUNTIME_DOWNLOAD).map { configuration ->
                configuration.incoming.artifactView {
                    componentFilter { id -> id !is ModuleComponentIdentifier || !isShaded(id.group, id.module) }
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
                exclude { dependency -> !dependency.isShaded() }
            }
        }
    }

    private fun ResolvedDependency.isShaded() =
        isShaded(moduleGroup, moduleName) ||
                moduleArtifacts.any { it.id.componentIdentifier is ProjectComponentIdentifier }

    /**
     * The surf modules (but not the surf forks of third-party libraries, `dev.slne.forks`) stay
     * in the server jar, as does what the bootstrap needs before anything is downloaded.
     */
    private fun isShaded(group: String, module: String) =
        group == "dev.slne.surf" || group.startsWith("dev.slne.surf.") || group to module in BOOTSTRAP_MODULES

    /** Builds the server jar and runs it with `java -jar`, which the agent in the manifest needs. */
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
            doFirst { runDirectory.mkdirs() }
            executable = launcher.get().executablePath.asFile.absolutePath
            argumentProviders += CommandLineArgumentProvider {
                listOf("-jar", shadowJar.get().archiveFile.get().asFile.absolutePath)
            }
        }
    }

    private companion object {
        const val LOG4J_CONFIG = "log4j2.xml"

        const val RUNTIME_DOWNLOAD = "runtimeDownload"
        const val JAR_RELOCATOR_RUNTIME = "jarRelocatorRuntime"
        const val DEPENDENCIES_FILE = "dependencies.txt"
        const val BOOTSTRAP_MODULE = "surf-api-minestom-server-bootstrap"
        const val BOOTSTRAP_AGENT = "dev.slne.surf.api.minestom.server.bootstrap.SurfMinestomBootstrap"
        const val DELEGATE_AGENTS_ATTRIBUTE = "Surf-Delegate-Agent-Classes"

        /** What the bootstrap needs before anything is downloaded. */
        val BOOTSTRAP_MODULES = setOf(
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
