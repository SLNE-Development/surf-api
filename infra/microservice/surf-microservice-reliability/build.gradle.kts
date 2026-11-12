@file:OptIn(ExperimentalAbiValidation::class)

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("dev.slne.surf.api.gradle.core")
    `java-test-fixtures`
}

kotlin {
    explicitApi()

    abiValidation {
        filters {
            exclude {
                annotatedWith.add("dev.slne.surf.microservice.api.InternalMicroserviceApi")
            }
        }
    }
}

dependencies {
    api(projects.surfApiMicroservice.surfApiMicroserviceDatabase)
    compileOnly(shadowed(projects.surfApiDatabaseR2dbc))
    compileOnly(projects.surfApiNats.surfApiNatsApi)

    testFixturesCompileOnly(projects.surfApiCore)
    testFixturesCompileOnly(shadowed(projects.surfApiDatabaseR2dbc))
    testFixturesCompileOnly(projects.surfApiNats.surfApiNatsApi)

    testImplementation(shadowed(projects.surfApiDatabaseR2dbc))
    testImplementation(shadowed(projects.surfApiNats.surfApiNatsPlatform.surfApiNatsStandalone))
    testImplementation(projects.surfApiNats.surfApiNatsTest.surfApiNatsTestCommon)
    testImplementation(libs.jnats)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.nats)
    testImplementation(libs.mariadb.jdbc)
    testImplementation(libs.postgresql.jdbc)
    testRuntimeOnly(projects.surfApiStandalone)
    testRuntimeOnly(projects.surfApiStandaloneTesting)
}

/*
 * surf-database and surf-nats are shaded jars that reference configurate as `dev.slne.surf.api.libs.configurate`, the
 * package a microservice's shadow jar relocates it to, while the published surf API artifacts use the original
 * package. The tests therefore run with the surf API and configurate bundled into one jar; the surf gradle plugin
 * applies the configurate relocation to every ShadowJar task.
 */
val surfRuntimeGroups = setOf("dev.slne.surf.api", "dev.slne.forks.configurate")

fun Project.isInfra() = projectDir.startsWith(rootProject.file("infra"))

fun testRuntimeArtifacts(surfRuntime: Boolean) = configurations.testRuntimeClasspath.map { configuration ->
    configuration.incoming.artifactView {
        componentFilter { id ->
            val isSurfRuntime = when (id) {
                is ModuleComponentIdentifier -> id.group in surfRuntimeGroups
                // surf-api's own modules, which this build resolves to its projects
                is ProjectComponentIdentifier -> id.build.buildPath == ":" && !project(id.projectPath).isInfra()
                else -> false
            }
            isSurfRuntime == surfRuntime
        }
    }.files
}

val testSurfRuntime = tasks.register<ShadowJar>("testSurfRuntime") {
    description = "Bundles the surf runtime for the tests with configurate relocated like in a microservice."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    archiveFileName = "test-surf-runtime.jar"
    destinationDirectory = layout.buildDirectory.dir("testSurfRuntime")
    configurations.set(emptyList())
    // Mapped to zip trees, the files lose the tasks that build the surf-api project jars.
    dependsOn(testRuntimeArtifacts(surfRuntime = true))
    from(testRuntimeArtifacts(surfRuntime = true).map { jars -> jars.map(::zipTree) })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesNotMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
}

tasks.test {
    useJUnitPlatform()
    classpath = sourceSets.test.get().output + sourceSets.main.get().output +
        files(testSurfRuntime) + files(testRuntimeArtifacts(surfRuntime = false))
}

tasks.named<KotlinCompile>("compileTestFixturesKotlin") {
    compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
}
