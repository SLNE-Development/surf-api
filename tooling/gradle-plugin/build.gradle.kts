import java.nio.file.Files

// region properties
val relocationPrefix = project.findProperty("relocationPrefix") as String
val mcVersion = project.findProperty("mcVersion") as String
val groupId = findProperty("group") as String
val javaVersion = findProperty("javaVersion") as String
val snapshot = (findProperty("snapshot") as String).toBooleanStrict()
val baseVersion = findProperty("version") as String
val processorVersion = findProperty("processorVersion") as String
// endregion

plugins {
    // Before kotlin-dsl: puts the Kotlin Gradle plugin of the version catalog on the classpath instead of the one
    // embedded in Gradle.
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `kotlin-dsl`

    id("com.gradle.plugin-publish") version "2.2.1"
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildconfig)
    idea
}

group = groupId
version = buildString {
    append(baseVersion)
    if (snapshot) append("-SNAPSHOT")
}

kotlin {
    jvmToolchain(javaVersion.toInt())
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://reposilite.slne.dev/public/") { name = "public" }
}

val pluginDependencies = listOf(
    libs.kotlin.gradle.plugin,
    libs.kotlin.all.open,
    libs.kotlin.no.arg,
    libs.kotlin.serialization,
    libs.shadow.gradle.plugin,
    libs.plugin.yml.paper.gradle.plugin,
    libs.ksp.gradle.plugin,
    libs.gremlin.gradle.plugin,
)

dependencies {
    compileOnly(gradleApi())
    pluginDependencies.forEach { dep -> api(dep) }

    implementation("com.palantir.javapoet:javapoet:0.22.0")
    implementation(libs.bundles.kotlin.serialization)

    // surf-nats: contract generation
    implementation(libs.wire.schema) {
        exclude(group = "com.google.guava")
        exclude(group = "com.palantir.javapoet")
        exclude(group = "com.squareup", module = "kotlinpoet")
    }
    implementation(libs.kotlinpoet)
    // Applied by the surf-nats plugin for Kotlin/JVM projects.
    implementation(libs.kotlin.serialization.plugin)
    // Applied by the surf-nats plugin to every project; only the conventions and the wiring come from here.
    implementation(libs.weaver.gradle.plugin)
    compileOnly(libs.kotlin.gradle.api)

    // surf-microservice: Docker tasks
    implementation(libs.bmuschko.docker.remote.api)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()

    // The microservice tests put the surf-api plugins on the plugin classpath of their TestKit builds. They are part
    // of this plugin, so that is its own runtime classpath.
    val pluginClasspath = sourceSets.main.map { it.runtimeClasspath }
    inputs.files(pluginClasspath)
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("surfApiClasspath")
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Dsurf.microservice.test.surfApiClasspath=${pluginClasspath.get().asPath}")
    }
}

testing {
    suites {
        // A Kotlin/JVM consumer that applies the surf-nats plugin without any surf-api platform plugin. Its plugin
        // jar brings the serialization plugin to the other tests.
        register<JvmTestSuite>("plainJvmTest") {
            useJUnitJupiter(libs.versions.junit.asProvider())
            dependencies {
                implementation(project())
                implementation(gradleApi())
                implementation(libs.kotlin.gradle.plugin)
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("plainJvmTest"))
}

gradlePlugin {
    plugins {
        create("settings") {
            id = "dev.slne.surf.api.gradle.settings"
            implementationClass = "dev.slne.surf.api.gradle.settings.SurfSettingsPlugin"
        }

        create("core") {
            id = "dev.slne.surf.api.gradle.core"
            implementationClass = "dev.slne.surf.api.gradle.platform.core.CoreSurfPlugin"
        }

        create("paper-plugin") {
            id = "dev.slne.surf.api.gradle.paper-plugin"
            implementationClass =
                "dev.slne.surf.api.gradle.platform.paper.plugin.PaperPluginSurfPlugin"
        }

        create("paper-raw") {
            id = "dev.slne.surf.api.gradle.paper-raw"
            implementationClass = "dev.slne.surf.api.gradle.platform.paper.raw.RawPaperSurfPlugin"
        }

        create("standalone") {
            id = "dev.slne.surf.api.gradle.standalone"
            implementationClass =
                "dev.slne.surf.api.gradle.platform.standalone.StandaloneSurfPlugin"
        }

        create("velocity") {
            id = "dev.slne.surf.api.gradle.velocity"
            implementationClass = "dev.slne.surf.api.gradle.platform.velocity.VelocitySurfPlugin"
        }

        create("minestom") {
            id = "dev.slne.surf.api.gradle.minestom"
            implementationClass = "dev.slne.surf.api.gradle.platform.minestom.MinestomSurfPlugin"
        }

        create("minestom-server") {
            id = "dev.slne.surf.api.gradle.minestom-server"
            implementationClass =
                "dev.slne.surf.api.gradle.platform.minestom.server.MinestomServerSurfPlugin"
        }

        create("minestom-relocations") {
            id = "dev.slne.surf.api.gradle.minestom-relocations"
            implementationClass =
                "dev.slne.surf.api.gradle.platform.minestom.MinestomRelocationsPlugin"
        }

        create("nats") {
            id = "dev.slne.surf.api.gradle.nats"
            implementationClass = "dev.slne.surf.nats.gradle.SurfNatsPlugin"
            displayName = "surf-nats"
            description = "Generates surf-nats Kotlin contracts from .proto files"
        }

        create("microservice") {
            id = "dev.slne.surf.api.gradle.microservice"
            implementationClass = "dev.slne.surf.microservice.gradle.MicroservicePlugin"
        }
    }

    publishing {
        repositories {
            maven("https://reposilite.slne.dev/releases/") {
                name = "slne-repository-releases"
                credentials {
                    username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                    password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
                }
            }
        }
    }
}

val fullVersion = version.toString()

val constantsOutputDir =
    layout.buildDirectory.dir("generated/dev/slne/surf/api/gradle/generated")
val generateConstants = tasks.register("generateConstants") {
    description = "Generates the Constants.kt file with build information."

    val outputFile = constantsOutputDir.map { it.file("Constants.kt") }

    inputs.property("relocationPrefix", relocationPrefix)
    inputs.property("javaVersion", javaVersion)
    inputs.property("mcVersion", mcVersion)
    inputs.property("libs.paper.api", libs.paper.api.get().toString())
    inputs.property("libs.canvas.api", libs.canvas.api.get().toString())
    inputs.property("libs.velocity.api", libs.velocity.api.get().toString())
    inputs.property("libs.versions.commandapiplugin", libs.versions.commandapiplugin.get())
    inputs.property("libs.versions.placeholder.api", libs.versions.placeholder.api.get())
    inputs.property(
        "libs.versions.luckpermsplugin.bukkit",
        libs.versions.luckpermsplugin.bukkit.get()
    )
    inputs.property(
        "libs.versions.packetevents",
        libs.versions.packetevents.plugin.get()
    )
    inputs.property("group", groupId)
    inputs.property("processorVersion", processorVersion)
    inputs.property("version", fullVersion)
    inputs.property("libs.versions.junit", libs.versions.junit.asProvider().get())
    inputs.property(
        "libs.versions.junit.platform.launcher",
        libs.versions.junit.platform.launcher.get()
    )
    inputs.property("libs.versions.mockk", libs.versions.mockk.get())
    inputs.property("libs.versions.lincheck", libs.versions.lincheck.get())
    inputs.property("libs.versions.mockbukkit", libs.versions.mockbukkit.get())
    inputs.property("libs.versions.minestom.testing", libs.versions.minestom.testing.get())
    inputs.property("libs.versions.kotlinxCoroutines", libs.versions.kotlinxCoroutines.get())
    inputs.property("libs.minestom.server", libs.minestom.server.get().toString())
    inputs.property("libs.versions.log4j", libs.versions.log4j.get())
    inputs.property("libs.versions.asm", libs.versions.asm.get())
    outputs.dir(constantsOutputDir)

    doLast {
        val content = """
            |package dev.slne.surf.api.gradle.generated
            |
            |internal object Constants {
            |    const val RELOCATION_PREFIX = "$relocationPrefix"
            |    const val SNAPSHOT_REPO_ID = "slne-repository-releases"
            |    const val SNAPSHOT_REPO = "https://reposilite.slne.dev/releases"
            |    const val PAPER_API = "${libs.paper.api.get()}"
            |    const val CANVAS_API = "${libs.canvas.api.get()}"
            |    const val VELOCITY_API = "${libs.velocity.api.get()}"
            |    const val SURF_PROCESSOR = "$groupId:surf-api-processor:$processorVersion${if (snapshot) "-SNAPSHOT" else ""}"
            |
            |    const val JAVA_VERSION = $javaVersion
            |    const val MINECRAFT_VERSION = "$mcVersion"
            |    const val SURF_API_VERSION = "+"
            |
            |    const val COMMAND_API_VERSION = "${libs.versions.commandapiplugin.get()}"
            |    const val PLACEHOLDER_API_VERSION = "${libs.versions.placeholder.api.get()}"
            |    const val LUCKPERMS_VERSION = "${libs.versions.luckpermsplugin.bukkit.get()}"
            |    const val PACKETEVENTS_VERSION = "${libs.versions.packetevents.plugin.get()}"
            |
            |    const val TEST_JUNIT_VERSION = "${libs.versions.junit.asProvider().get()}"
            |    const val TEST_JUNIT_PLATFORM_LAUNCHER_VERSION = "${libs.versions.junit.platform.launcher.get()}"
            |    const val TEST_MOCKK_VERSION = "${libs.versions.mockk.get()}"
            |    const val TEST_LINCHECK_VERSION = "${libs.versions.lincheck.get()}"
            |    const val TEST_MOCKBUKKIT_VERSION = "${libs.versions.mockbukkit.get()}"
            |    const val TEST_MINESTOM_TESTING_VERSION = "${libs.versions.minestom.testing.get()}"
            |    const val TEST_COROUTINES_VERSION = "${libs.versions.kotlinxCoroutines.get()}"
            |
            |    const val MINESTOM = "${libs.minestom.server.get()}"
            |    const val LOG4J_VERSION = "${libs.versions.log4j.get()}"
            |    const val ASM_VERSION = "${libs.versions.asm.get()}"
            |
            |    const val SURF_API_FULL_VERSION = "$fullVersion"
            |}
        """.trimMargin()

        Files.createDirectories(constantsOutputDir.get().asFile.toPath())
        outputFile.get().asFile.writeText(content)
    }
}

// surf-nats: the proto files the plugin bundles are the ones of the type modules.
val bundledProtoDirs = listOf(
    layout.projectDirectory.dir("../../infra/nats/surf-nats-types/surf-nats-types-common/src/main/proto"),
    layout.projectDirectory.dir("../../infra/nats/surf-nats-types/surf-nats-types-paper/src/main/proto"),
)

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }
    forClass("dev.slne.surf.nats.gradle", "SurfNatsPluginVersion") {
        buildConfigField("GROUP", provider { project.group.toString() })
        buildConfigField("VERSION", provider { project.version.toString() })
        buildConfigField("KOTLINX_SERIALIZATION_VERSION", libs.versions.kotlinx.serialization)
    }
    forClass("dev.slne.surf.nats.gradle.schema", "BundledProtoPaths") {
        buildConfigField("PATHS", provider {
            bundledProtoDirs.flatMap { dir ->
                val root = dir.asFile
                fileTree(root) { include("**/*.proto") }.files.map { it.relativeTo(root).invariantSeparatorsPath }
            }.sorted()
        })
    }
    forClass("dev.slne.surf.microservice.gradle.generated", "Constants") {
        buildConfigField(
            "SURF_MICROSERVICE_VERSION",
            provider { project.version.toString() }.map { "${it.substringBefore('.')}.+" }
        )
    }
}

sourceSets {
    main {
        kotlin.srcDir(generateConstants.map { it.outputs })

        // Same protocol rules as the surf-nats-core runtime (also compiled there for JVM and JS).
        kotlin.srcDir("../../infra/nats/surf-nats-core/src/protocol/kotlin")
        bundledProtoDirs.forEach { resources.srcDir(it) }
    }
    test {
        kotlin.srcDir("../../infra/nats/surf-nats-core/src/protocolTest/kotlin")
    }
}

idea {
    module {
        generatedSourceDirs.add(constantsOutputDir.get().asFile)

        // The KSP processor is a project of the main build.
        excludeDirs.add(file("processor"))
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
