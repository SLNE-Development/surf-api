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

@Suppress("UNCHECKED_CAST")
val shadedPackages = rootProject.extra["shadedPackages"] as Map<String, String>

val redissonPackagePath = "org/redisson"
val redissonRelocationPath = shadedPackages.getValue("org.redisson").replace('.', '/')
val relocationExtraDepth =
    redissonRelocationPath.count { it == '/' } - redissonPackagePath.count { it == '/' }

val shadedPackageRewrites = shadedPackages
    .flatMap { (from, to) ->
        listOf(from to to, from.replace('.', '/') to to.replace('.', '/'))
    }
    .distinctBy { it.first }

fun String.applyShadedPackages(): String {
    var rewritten = this
    for ((from, to) in shadedPackageRewrites) {
        if (rewritten.contains(from)) rewritten = rewritten.replace(from, to)
    }
    return rewritten
}

fun redissonArtifact(configurationName: String, classifier: String): Configuration {
    val configuration = configurations.create(configurationName) {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }

    dependencies {
        add(configurationName, variantOf(libs.redisson) { classifier(classifier) })
    }

    return configuration
}

fun registerRedissonRelocation(
    taskName: String,
    source: Configuration,
    outputDirectory: String,
    textFileSuffix: String,
    rewriteLinkDepth: Boolean,
) = tasks.register<Sync>(taskName) {
    description = "Rewrites Redisson's $textFileSuffix files onto the shaded package names."
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    into(layout.buildDirectory.dir(outputDirectory))
    includeEmptyDirs = false
    filteringCharset = Charsets.UTF_8.name()

    inputs.property("shadedPackageRewrites", shadedPackageRewrites.toString())
    inputs.property("rewriteLinkDepth", rewriteLinkDepth)

    from(source.elements.map { jars -> jars.map(::zipTree) }) {
        include("$redissonPackagePath/**")

        eachFile {
            val depth = relativePath.segments.size - 1

            if (name.endsWith(textFileSuffix)) {
                filter { line ->
                    val relinked = if (rewriteLinkDepth) {
                        line.replace(
                            "../".repeat(depth),
                            "../".repeat(depth + relocationExtraDepth),
                        )
                    } else {
                        line
                    }

                    relinked.applyShadedPackages()
                }
            }

            path = redissonRelocationPath + path.removePrefix(redissonPackagePath)
        }
    }
}

dependencies {
    api(libs.redisson) {
        exclude("org.slf4j")
        exclude("org.reactivestreams")
        exclude("io.projectreactor", "reactor-core")
    }

    testImplementation(kotlin("test-junit5"))
}

val relocateRedissonJavadoc = registerRedissonRelocation(
    taskName = "relocateRedissonJavadoc",
    source = redissonArtifact("redissonJavadoc", "javadoc"),
    outputDirectory = "docs/redisson-javadoc",
    textFileSuffix = ".html",
    rewriteLinkDepth = true,
)

val relocateRedissonSources = registerRedissonRelocation(
    taskName = "relocateRedissonSources",
    source = redissonArtifact("redissonSources", "sources"),
    outputDirectory = "docs/redisson-sources",
    textFileSuffix = ".java",
    rewriteLinkDepth = false,
)

tasks.javadoc {
    setSource(files())
    setDestinationDir(layout.buildDirectory.dir("docs/javadoc-unused").get().asFile)
}

tasks.named<Jar>("javadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(tasks.named<DokkaGenerateTask>("dokkaGeneratePublicationJavadoc").flatMap { it.outputDirectory })
    from(relocateRedissonJavadoc)
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(relocateRedissonSources)
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
