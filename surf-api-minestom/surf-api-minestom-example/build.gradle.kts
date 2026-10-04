import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    `core-convention`
}

description = "surf-api-minestom-example"

val luckPerms = findProject(":surf-api-minestom:surf-api-minestom-server-luckperms")

dependencies {
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

tasks {
    shadowJar {
        archiveClassifier = "all"
        // log4j-core and the terminal console appender each ship a plugin cache, which have to be
        // merged; duplicates have to reach the transformer for that instead of being dropped
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        transform<Log4j2PluginsCacheFileTransformer>()
        manifest {
            attributes["Main-Class"] = mainClassName
            // LuckPerms adds the libraries it downloads to the class path through this agent
            attributes["Launcher-Agent-Class"] = "me.lucko.luckperms.minestom.dependencies.LuckPermsAgent"
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
