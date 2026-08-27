@file:OptIn(ExperimentalAbiValidation::class)

import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("dev.slne.surf.api.gradle.core")
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
}

surfCoreApi {
    withApiValidation()
}

kotlin {
    abiValidation {
        filters {
            exclude {
                annotatedWith.add("dev.slne.surf.redis.util.InternalRedisAPI")
            }
        }
    }
}

val redissonRelocation = "${rootProject.extra["libsRelocationBase"]}redisson"

val redissonJavadoc = configurations.create("redissonJavadoc") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    api(libs.redisson) {
        exclude("org.slf4j")
        exclude("org.reactivestreams")
        exclude("io.projectreactor", "reactor-core")
    }

    redissonJavadoc(variantOf(libs.redisson) { classifier("javadoc") })

    testImplementation(kotlin("test-junit5"))
}

val relocateRedissonJavadoc = tasks.register<Sync>("relocateRedissonJavadoc") {
    description = "Rewrites Redisson's javadoc onto the package names used in the shaded jar."
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    val fromPath = "org/redisson"
    val toPath = redissonRelocation.replace('.', '/')
    val extraDepth = toPath.count { it == '/' } - fromPath.count { it == '/' }

    into(layout.buildDirectory.dir("docs/redisson-javadoc"))
    includeEmptyDirs = false
    filteringCharset = Charsets.UTF_8.name()

    from(redissonJavadoc.elements.map { jars -> jars.map(::zipTree) }) {
        include("$fromPath/**")

        eachFile {
            val depth = relativePath.segments.size - 1

            if (name.endsWith(".html")) {
                filter { line ->
                    line.replace("../".repeat(depth), "../".repeat(depth + extraDepth))
                        .replace("$fromPath/", "$toPath/")
                        .replace("org.redisson", redissonRelocation)
                }
            }

            path = toPath + path.removePrefix(fromPath)
        }
    }
}

tasks.javadoc {
    setSource(files())
    setDestinationDir(layout.buildDirectory.dir("docs/javadoc-unused").get().asFile)
}

tasks.named<Jar>("javadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(tasks.named<DokkaGenerateTask>("dokkaGeneratePublicationJavadoc").flatMap { it.outputDirectory })
    from(relocateRedissonJavadoc)
}

java {
    withSourcesJar()
    withJavadocJar()
}

val shadowComponent = components["shadow"] as AdhocComponentWithVariants
shadowComponent.addVariantsFromConfiguration(configurations["sourcesElements"]) {}
shadowComponent.addVariantsFromConfiguration(configurations["javadocElements"]) {}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])
        }
    }

    repositories {
        slneReleases()
    }
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = false
}
