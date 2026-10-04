import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    `core-convention`
    id("xyz.jpenilla.gremlin-gradle")
}

description = "surf-api-minestom-example"

val luckPerms = findProject(":surf-api-minestom:surf-api-minestom-server-luckperms")

dependencies {
    implementation(projects.surfApiMinestom.surfApiMinestomServerBootstrap)
    implementation(projects.surfApiMinestom.surfApiMinestomServer)
    implementation(projects.surfApiMinestom.surfApiMinestomServerSignedChat)
    implementation(projects.surfApiMinestom.surfApiMinestomServerSpark)
    implementation(projects.surfApiMinestom.surfApiMinestomServerNpc)
    implementation(projects.surfApiMinestom.surfApiMinestomServerPlayerVisibility)
    implementation(projects.surfApiMinestom.surfApiMinestomServerConsole)
    implementation(projects.surfApiMinestom.surfApiMinestomServerPlugins)

    // Shaded into the server, where the plugin feature finds it on the class path
    implementation(projects.surfApiMinestom.surfApiMinestomExamplePlugin)
    if (luckPerms != null) implementation(luckPerms)

    implementation(libs.minestom.server)
    runtimeOnly(libs.log4j.slf4j2.impl)
}

// The example decides at runtime whether LuckPerms is on the class path
sourceSets.main {
    kotlin.srcDir(if (luckPerms != null) "src/luckperms/kotlin" else "src/noLuckperms/kotlin")
}

val mainClassName = "dev.slne.surf.api.minestom.example.ExampleServerKt"

// Like a server built with dev.slne.surf.api.gradle.minestom-server: the jar contains the project
// modules and the bootstrap, which downloads every other library into run/libraries/ on startup
gremlin {
    defaultGremlinRuntimeDependency = false
    // gremlin's default ASM cannot read the class files of current Java versions
    defaultJarRelocatorDependencies = false
}

dependencies {
    jarRelocatorRuntime("me.lucko:jar-relocator:1.7")
    jarRelocatorRuntime(libs.asm)
    jarRelocatorRuntime("org.ow2.asm:asm-commons:${libs.versions.asm.get()}")
}

configurations.runtimeDownload {
    extendsFrom(configurations.implementation.get(), configurations.runtimeOnly.get())
    shouldResolveConsistentlyWith(configurations.runtimeClasspath.get())
    exclude(group = "xyz.jpenilla", module = "gremlin-runtime")
    exclude(group = "org.jspecify", module = "jspecify")
}

tasks {
    writeDependencies {
        // The relocations core-convention applies to the shadow jar
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("net.kyori.adventure.nbt", "$relocationPrefix.kyori.nbt") {
            excludes.add("net.kyori.adventure.nbt.api.**")
        }
        relocate("org.spongepowered.configurate", "$relocationPrefix.configurate")
    }

    shadowJar {
        archiveClassifier = "all"
        dependencies {
            exclude { dependency ->
                dependency.moduleGroup != "xyz.jpenilla" && dependency.moduleGroup != "org.jspecify" &&
                        dependency.moduleArtifacts.none { it.id.componentIdentifier is ProjectComponentIdentifier }
            }
        }
        // log4j-core and the terminal console appender each ship a plugin cache, which have to be
        // merged; duplicates have to reach the transformer for that instead of being dropped
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        transform<Log4j2PluginsCacheFileTransformer>()
        manifest {
            attributes["Main-Class"] = mainClassName
            attributes["Launcher-Agent-Class"] = "dev.slne.surf.api.minestom.server.bootstrap.SurfMinestomBootstrap"
            // LuckPerms adds the libraries it downloads to the class path through this agent
            if (luckPerms != null) {
                attributes["Surf-Delegate-Agent-Classes"] = "me.lucko.luckperms.minestom.dependencies.LuckPermsAgent"
            }
            attributes["Multi-Release"] = "true"
        }
    }

    register<Exec>("runServer") {
        group = "application"
        description = "Builds the example server and runs it in run/"
        dependsOn(shadowJar)

        val runDirectory = layout.projectDirectory.dir("run").asFile
        val launcher = project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion = JavaLanguageVersion.of(project.findProperty("javaVersion") as String)
        }

        workingDir = runDirectory
        standardInput = System.`in`
        doFirst { runDirectory.mkdirs() }
        executable = launcher.get().executablePath.asFile.absolutePath
        argumentProviders += CommandLineArgumentProvider {
            listOf("-jar", shadowJar.get().archiveFile.get().asFile.absolutePath)
        }
    }
}
