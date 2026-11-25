import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

plugins {
    id("dev.slne.surf.api.gradle.core")
    `api-validation`
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
}

dependencies {
    api(libs.exposed.core)
    api(libs.exposed.r2dbc)
    api(libs.r2dbc.spi)
    implementation(libs.bundles.exposed)
    shadow(libs.bundles.exposed)
    shadow(libs.r2dbc.spi)
    implementation(libs.r2dbc.pool)
    implementation(libs.bundles.databaseDriver)
    implementation(libs.bundles.migration)

    compileOnly(libs.spring.context)
    compileOnly(libs.spring.boot.autoconfigure)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.kotlinxCoroutines.test)
    testImplementation(projects.surfApiStandaloneTesting)
    testImplementation(libs.spring.context)
    testImplementation(libs.spring.boot.autoconfigure)
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
    "io.r2dbc.pool",
    "io.netty",
    "org.mariadb",
    "com.ongres",
    "org.jspecify",
    "reactor.netty",
    "reactor.pool",
    "org.flywaydb",
    "org.postgresql",
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
        if (rewritten.contains(from)) {
            rewritten = rewritten.replace(from, to)
        }
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
        for (module in modules) {
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
): TaskProvider<Sync> {
    return tasks.register<Sync>(taskName) {
        description = "Rewrites bundled dependency documentation onto the shaded package names."
        group = JavaBasePlugin.DOCUMENTATION_GROUP

        into(layout.buildDirectory.dir(outputDirectory))
        includeEmptyDirs = false
        filteringCharset = Charsets.UTF_8.name()

        inputs.property("relocationRewrites", relocationRewrites.toString())
        inputs.property("rewriteLinkDepth", rewriteLinkDepth)

        from(source.elements.map { jars -> jars.map(::zipTree) }) {
            for (pattern in includes) {
                include(pattern)
            }

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
}

val sourceDocModules = listOf(
    libs.r2dbc.pool,
    libs.r2dbc.mariadb,
)

val javadocDocModules = listOf(
    libs.r2dbc.pool,
    libs.r2dbc.mariadb,
)

val relocateDependencySources = registerDocsRelocation(
    taskName = "relocateDependencySources",
    source = docsConfiguration("dependencyDocsSources", "sources", sourceDocModules),
    outputDirectory = "docs/relocated-sources",
    includes = listOf("io/r2dbc/**", "org/mariadb/**"),
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

    from(
        tasks.named<DokkaGenerateTask>("dokkaGeneratePublicationJavadoc")
            .flatMap { it.outputDirectory },
    )
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

/**
 * The PostgreSQL R2DBC driver's jar from the runtime classpath. The shadow jar bundles it outside
 * the dependency set that auto relocation scans, so the driver keeps its own packages: Exposed, a
 * dependency of the jar, refers to the driver's `io.r2dbc.postgresql.codec.Json`. The driver's
 * references to the other bundled libraries are still relocated.
 */
val postgresDriverJars = configurations.runtimeClasspath.map { configuration ->
    configuration.incoming.artifactView {
        componentFilter { id ->
            id is ModuleComponentIdentifier &&
                id.group == "org.postgresql" &&
                id.module == "r2dbc-postgresql"
        }
    }.files
}

/**
 * Merges `META-INF/services` files of all bundled jars. The service file transformer only sees
 * duplicates that are not dropped first, so they must be included for those paths; without this
 * only one jar's entries survive (Flyway then loses its core plugins and fails on load). Every
 * other duplicate is still excluded.
 */
fun ShadowJar.mergeAllServiceFiles() {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesNotMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
}

tasks {
    shadowJar {
        relocationPrefix = "dev.slne.surf.database.libs"
        enableAutoRelocation = true
        dependencies {
            exclude(dependency("org.jetbrains.exposed:.*:.*"))
            exclude(dependency("io.r2dbc:r2dbc-spi:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-datetime.*:.*"))
            exclude(dependency("org.postgresql:r2dbc-postgresql:.*"))
        }
        from(postgresDriverJars.map { jars -> jars.map(::zipTree) })
        mergeAllServiceFiles()
    }

    test {
        useJUnitPlatform()
    }
}

/**
 * Smoke tests that run against the shaded jar only: no `main` output and no unshaded Flyway,
 * drivers or R2DBC pool on the classpath. Exposed and r2dbc-spi come as the jar's dependencies, as
 * for a consumer. The surf runtime APIs are taken from what the surf gradle plugin adds to `test`.
 */
val shadowTestSourceSet = sourceSets.create("shadowTest") {
    // The SQL is served by an isolated class loader in the test, so it must not be on the test
    // classpath.
    resources.setSrcDirs(emptyList<Any>())
}
val shadowTestPluginSql = layout.projectDirectory.dir("src/shadowTest/resources")
val shadowJarFile = tasks.shadowJar.flatMap { it.archiveFile }

fun surfApiDependencies(from: String): Provider<List<Dependency>> {
    return provider {
        configurations[from].dependencies.filter { it.group == "dev.slne.surf.api" }
    }
}

configurations.named(shadowTestSourceSet.implementationConfigurationName) {
    withDependencies {
        addAllLater(surfApiDependencies(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME))
    }
}
configurations.named(shadowTestSourceSet.runtimeOnlyConfigurationName) {
    withDependencies {
        addAllLater(surfApiDependencies(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME))
    }
}

dependencies {
    add(shadowTestSourceSet.implementationConfigurationName, files(shadowJarFile))
    add(shadowTestSourceSet.implementationConfigurationName, libs.bundles.exposed)
    add(shadowTestSourceSet.implementationConfigurationName, libs.r2dbc.spi)
    add(shadowTestSourceSet.implementationConfigurationName, libs.junit.jupiter)
    add(shadowTestSourceSet.implementationConfigurationName, libs.testcontainers.junit)
    add(shadowTestSourceSet.implementationConfigurationName, libs.testcontainers.mariadb)
    add(shadowTestSourceSet.implementationConfigurationName, libs.testcontainers.postgresql)
    add(shadowTestSourceSet.implementationConfigurationName, libs.kotlinxCoroutines.test)
    add(shadowTestSourceSet.implementationConfigurationName, libs.spring.context)
    add(shadowTestSourceSet.implementationConfigurationName, libs.spring.boot.autoconfigure)
    add(
        shadowTestSourceSet.implementationConfigurationName,
        "org.jetbrains.kotlin:kotlin-test-junit5",
    )
    add(
        shadowTestSourceSet.runtimeOnlyConfigurationName,
        "org.junit.platform:junit-platform-launcher",
    )
    add(shadowTestSourceSet.runtimeOnlyConfigurationName, libs.slf4j)
}

/**
 * The shaded jar references configurate as `dev.slne.surf.api.libs.configurate` (relocated by the
 * surf gradle plugin, matching the surf runtime on a server), while the published surf API
 * artifacts use the original package. This task bundles the surf API and configurate artifacts of
 * the shadowTest runtime; the surf gradle plugin applies the same configurate relocation to every
 * `ShadowJar` task.
 */
val surfRuntimeGroups = setOf("dev.slne.surf.api", "dev.slne.forks.configurate")

/**
 * Libraries the surf runtime relocates into its own packages. surf-api's classes in the bundle
 * refer to the relocated names, so the libraries have to be bundled and relocated with them.
 */
val relocatedSurfLibraries = setOf("net.kyori:adventure-nbt")

fun Project.isInfra(): Boolean = projectDir.startsWith(rootProject.file("infra"))
val shadowTestRuntimeClasspath = configurations.named(
    shadowTestSourceSet.runtimeClasspathConfigurationName,
)

fun shadowTestRuntimeArtifacts(surfRuntime: Boolean): Provider<FileCollection> {
    return shadowTestRuntimeClasspath.map { configuration ->
        configuration.incoming.artifactView {
            componentFilter { id ->
                val isSurfRuntime = when (id) {
                    is ModuleComponentIdentifier ->
                        id.group in surfRuntimeGroups ||
                            "${id.group}:${id.module}" in relocatedSurfLibraries
                    // surf-api's own modules, which this build resolves to its projects
                    is ProjectComponentIdentifier ->
                        id.build.buildPath == ":" && !project(id.projectPath).isInfra()

                    else -> false
                }
                isSurfRuntime == surfRuntime
            }
        }.files
    }
}

val shadowTestSurfRuntime = tasks.register<ShadowJar>("shadowTestSurfRuntime") {
    description =
        "Bundles the surf runtime for shadowTest with configurate relocated like on a server."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    archiveFileName = "shadow-test-surf-runtime.jar"
    destinationDirectory = layout.buildDirectory.dir("shadowTest")
    configurations.set(emptyList())
    // Mapped to zip trees, the files lose the tasks that build the surf-api project jars.
    dependsOn(shadowTestRuntimeArtifacts(surfRuntime = true))
    from(shadowTestRuntimeArtifacts(surfRuntime = true).map { jars -> jars.map(::zipTree) })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    mergeAllServiceFiles()
}

val shadowTest = tasks.register<Test>("shadowTest") {
    description = "Runs plugin migrations from the shaded jar with an isolated plugin class loader."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    testClassesDirs = shadowTestSourceSet.output.classesDirs
    classpath = shadowTestSourceSet.output +
        files(shadowTestSurfRuntime) +
        files(shadowTestRuntimeArtifacts(surfRuntime = false))
    useJUnitPlatform()

    inputs.dir(shadowTestPluginSql).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("surf.shadowTest.pluginSql", shadowTestPluginSql.asFile.absolutePath)
}

tasks.check {
    dependsOn(shadowTest)
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
