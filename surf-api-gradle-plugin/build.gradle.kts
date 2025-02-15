// region properties
val relocationPrefix: String by project
val mcVersion: String by project
val groupId = findProperty("group") as String
val javaVersion: String by project
// endregion

plugins {
    `java-library`
    `kotlin-dsl`

    id("com.gradle.plugin-publish") version "1.3.0"
//    alias(libs.plugins.maven.repo.auth)
}

group = groupId
version = "$mcVersion-1.0.96"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val pluginDependencies = listOf(
    libs.kotlin.gradle.plugin,
    libs.kotlin.all.open,
    libs.kotlin.no.arg,
    libs.kotlin.serialization,
    libs.shadow.gradle.plugin,
    libs.run.paper.gradle.plugin,
    libs.plugin.yml.paper.gradle.plugin
)

dependencies {
    compileOnly(gradleApi())
    pluginDependencies.forEach { dep -> api(dep) }

    implementation("com.palantir.javapoet:javapoet:0.6.0")
}


gradlePlugin {
    plugins {
        create("core") {
            id = "dev.slne.surf.surfapi.gradle.core"
            implementationClass = "dev.slne.surf.surfapi.gradle.platform.core.CoreSurfPlugin"
        }

        create("paper-plugin") {
            id = "dev.slne.surf.surfapi.gradle.paper-plugin"
            implementationClass = "dev.slne.surf.surfapi.gradle.platform.paper.plugin.PaperPluginSurfPlugin"
        }
        create("paper-raw") {
            id = "dev.slne.surf.surfapi.gradle.paper-raw"
            implementationClass = "dev.slne.surf.surfapi.gradle.platform.paper.raw.RawPaperSurfPlugin"
        }

        create("standalone") {
            id = "dev.slne.surf.surfapi.gradle.standalone"
            implementationClass = "dev.slne.surf.surfapi.gradle.platform.standalone.StandaloneSurfPlugin"
        }

        create("velocity") {
            id = "dev.slne.surf.surfapi.gradle.velocity"
            implementationClass = "dev.slne.surf.surfapi.gradle.platform.velocity.VelocitySurfPlugin"
        }
    }

    publishing {
        repositories {
            maven("https://repo.slne.dev/repository/maven-releases/") {
                name = "maven-releases"
                credentials {
                    val getenv = System.getenv("SLNE_RELEASES_REPO_USERNAME")

                    System.err.println("Username: $getenv")

                    username = getenv
                    password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
                }
            }
        }
    }
}

val generatedConstantsDir = layout.buildDirectory.dir("generated/source/constants")
val generateConstantsTask by tasks.register("generateConstants") {
    val outputFile = generatedConstantsDir.map { it.file("Constants.kt") }

    outputs.file(outputFile)

    doLast {
        //language=kotlin
        val content = """
            package dev.slne.surf.surfapi.gradle.generated
            
            internal object Constants {
                const val RELOCATION_PREFIX = "$relocationPrefix"
                const val SNAPSHOT_REPO_ID = "maven-snapshots"
                const val SNAPSHOT_REPO = "https://repo.slne.dev/repository/maven-snapshots"
                const val PAPER_API = "${libs.paper.api.get()}"
                const val VELOCITY_API = "${libs.velocity.api.get()}"
                const val AUTO_SERVICE_ANNOTATIONS = "${libs.auto.service.annotations.get()}"
                const val AUTO_SERVICE = "${libs.auto.service.asProvider().get()}"
                
                const val JAVA_VERSION = $javaVersion
                const val MINECRAFT_VERSION = "$mcVersion"
                const val SURF_API_VERSION = "$mcVersion+"
            }
        """.trimIndent()

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    outputs.upToDateWhen { false }
}

afterEvaluate {
    generateConstantsTask.actions.forEach {
        it.execute(generateConstantsTask)
    }
}

sourceSets {
    main {
        java.srcDir(generatedConstantsDir)
    }
}