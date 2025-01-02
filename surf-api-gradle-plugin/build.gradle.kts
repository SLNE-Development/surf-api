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
    alias(libs.plugins.maven.repo.auth)
}

group = groupId
version = "$mcVersion-1.0.32-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val pluginDependencies = listOf(
    libs.kotlin.gradle.plugin,
    libs.kotlin.all.open,
    libs.kotlin.no.arg,
    libs.maven.repo.auth,
    libs.shadow.gradle.plugin,
    libs.run.paper.gradle.plugin,
    libs.plugin.yml.paper.gradle.plugin,
)

dependencies {
    compileOnly(gradleApi())
    pluginDependencies.forEach { dep -> implementation(dep) }
// https://mvnrepository.com/artifact/com.squareup/javapoet
    implementation("com.squareup:javapoet:1.13.0")
}


gradlePlugin {
    plugins {
        create("gradle") {
            id = "dev.slne.surf.surfapi.gradle"
            implementationClass = "dev.slne.surf.surfapi.gradle.SurfApiPlugin"
        }
    }

    publishing {
        repositories {
            maven("https://repo.slne.dev/repository/maven-unsafe/") { name = "maven-unsafe" }
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
                const val RELOCATION_PREFIX = "${findProperty("relocationPrefix")}"
                const val SNAPSHOT_REPO_ID = "maven-snapshots"
                const val SNAPSHOT_REPO = "https://repo.slne.dev/repository/maven-snapshots"
                const val PAPER_API = "${libs.paper.api.get()}"
                const val VELOCITY_API = "${libs.velocity.api.get()}"
                
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