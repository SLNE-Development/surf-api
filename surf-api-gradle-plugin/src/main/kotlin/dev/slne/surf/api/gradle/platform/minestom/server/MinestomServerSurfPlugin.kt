package dev.slne.surf.api.gradle.platform.minestom.server

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.generators.GeneratePluginFile
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingConfigurer
import dev.slne.surf.api.gradle.platform.core.AbstractCoreSurfPlugin
import org.gradle.api.Project
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
import org.gradle.process.CommandLineArgumentProvider

/**
 * A runnable surf Minestom server: the surf api server, Minestom, the features asked for and the
 * project's own code, shaded into one executable jar.
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

        val agent = features.firstNotNullOfOrNull(MinestomServerFeature::agentClass)
        tasks.named<ShadowJar>("shadowJar") {
            if (mainClass != null) manifest.attributes["Main-Class"] = mainClass
            if (agent != null) manifest.attributes["Launcher-Agent-Class"] = agent
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
