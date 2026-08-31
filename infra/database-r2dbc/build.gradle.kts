import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import dev.slne.surf.api.gradle.util.slneReleases
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

plugins {
    id("dev.slne.surf.api.gradle.core") version "+"
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
}

surfCoreApi {
    withApiValidation()
}

group = "dev.slne.surf"
version = findProperty("version") as String

dependencies {
    implementation(libs.bundles.exposed)
    implementation(libs.r2dbc.pool)
    implementation(libs.bundles.databaseDriver)

    compileOnly(libs.surf.microservice)
}

configurations.runtimeClasspath {
    exclude("io.projectreactor", "reactor-core")
    exclude("org.reactivestreams")
    exclude("org.slf4j")
    exclude("org.jetbrains.kotlin")
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-reactive")
    exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
}

shadow {
    addShadowVariantIntoJavaComponent = false
}

val relocationPrefix = "dev.slne.surf.database.libs"
val relocationPrefixPath = relocationPrefix.replace('.', '/')

val relocationExtraDepth = relocationPrefixPath.split('/').size

val shadedRoots = listOf(
    "org.jetbrains.exposed",
    "io.r2dbc",
    "io.netty",
    "org.mariadb",
    "com.ongres",
    "org.jspecify",
    "kotlinx.datetime",
    "reactor.netty",
    "reactor.pool",
)

val relocationRewrites = shadedRoots.flatMap { root ->
    listOf(
        root to "$relocationPrefix.$root",
        root.replace('.', '/') to "$relocationPrefixPath/${root.replace('.', '/')}",
    )
}

fun String.applyRelocations(): String {
    var rewritten = this
    for ((from, to) in relocationRewrites) {
        if (rewritten.contains(from)) rewritten = rewritten.replace(from, to)
    }
    return rewritten
}

fun docsConfiguration(
    configurationName: String,
    classifier: String,
    modules: List<Provider<MinimalExternalModuleDependency>>,
): Configuration {
    val configuration = configurations.create(configurationName) {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }

    dependencies {
        modules.forEach { module ->
            add(configurationName, variantOf(module) { classifier(classifier) })
        }
    }

    return configuration
}

fun registerDocsRelocation(
    taskName: String,
    source: Configuration,
    outputDirectory: String,
    includes: List<String>,
    textFileSuffixes: List<String>,
    rewriteLinkDepth: Boolean,
) = tasks.register<Sync>(taskName) {
    description = "Rewrites bundled dependency documentation onto the shaded package names."
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    into(layout.buildDirectory.dir(outputDirectory))
    includeEmptyDirs = false
    filteringCharset = Charsets.UTF_8.name()

    inputs.property("relocationRewrites", relocationRewrites.toString())
    inputs.property("rewriteLinkDepth", rewriteLinkDepth)

    from(source.elements.map { jars -> jars.map(::zipTree) }) {
        includes.forEach { include(it) }

        eachFile {
            val depth = relativePath.segments.size - 1

            if (textFileSuffixes.any { name.endsWith(it) }) {
                filter { line ->
                    val relinked = if (rewriteLinkDepth) {
                        line.replace(
                            "../".repeat(depth),
                            "../".repeat(depth + relocationExtraDepth),
                        )
                    } else {
                        line
                    }

                    relinked.applyRelocations()
                }
            }

            path = "$relocationPrefixPath/$path"
        }
    }
}

val sourceDocModules = listOf(
    libs.exposed.core,
    libs.exposed.r2dbc,
    libs.exposed.java.time,
    libs.exposed.json,
    libs.exposed.migration.r2dbc,
    libs.r2dbc.pool,
    libs.r2dbc.mariadb,
    libs.r2dbc.postgresql,
)

val javadocDocModules = listOf(
    libs.r2dbc.pool,
    libs.r2dbc.mariadb,
    libs.r2dbc.postgresql,
)

val relocateDependencySources = registerDocsRelocation(
    taskName = "relocateDependencySources",
    source = docsConfiguration("dependencyDocsSources", "sources", sourceDocModules),
    outputDirectory = "docs/relocated-sources",
    includes = listOf("org/jetbrains/exposed/**", "io/r2dbc/**", "org/mariadb/**"),
    textFileSuffixes = listOf(".kt", ".java"),
    rewriteLinkDepth = false,
)

val relocateDependencyJavadoc = registerDocsRelocation(
    taskName = "relocateDependencyJavadoc",
    source = docsConfiguration("dependencyDocsJavadoc", "javadoc", javadocDocModules),
    outputDirectory = "docs/relocated-javadoc",
    includes = listOf("io/r2dbc/**", "org/mariadb/**"),
    textFileSuffixes = listOf(".html"),
    rewriteLinkDepth = true,
)

tasks.javadoc {
    setSource(files())
    setDestinationDir(layout.buildDirectory.dir("docs/javadoc-unused").get().asFile)
}

tasks.named<Jar>("javadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(tasks.named<DokkaGenerateTask>("dokkaGeneratePublicationJavadoc").flatMap { it.outputDirectory })
    from(relocateDependencyJavadoc)
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(relocateDependencySources)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    shadowJar {
        relocationPrefix = "dev.slne.surf.database.libs"
        enableAutoRelocation = true
    }

    test {
        useJUnitPlatform()
    }
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


/**
 * Only publish the shadow variant; the auto-created pluginMaven publication would claim the same
 * coordinates.
 */
afterEvaluate {
    tasks.withType<AbstractPublishToMaven>().configureEach {
        onlyIf("only the shadow publication owns these coordinates") {
            publication.name != "pluginMaven"
        }
    }
}
