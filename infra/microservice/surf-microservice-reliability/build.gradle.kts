import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("dev.slne.surf.api.gradle.core")
    `java-test-fixtures`
}

val surfDatabaseR2dbc = "dev.slne.surf:surf-database-r2dbc:2.+"
val surfNatsVersion = "1.+"

dependencies {
    api(projects.surfMicroserviceDatabase)
    compileOnly(surfDatabaseR2dbc)
    compileOnly("dev.slne.surf.nats:surf-nats-api:$surfNatsVersion")

    testFixturesCompileOnly("dev.slne.surf.api:surf-api-core:+")
    testFixturesCompileOnly(surfDatabaseR2dbc)
    testFixturesCompileOnly("dev.slne.surf.nats:surf-nats-api:$surfNatsVersion")

    testImplementation(surfDatabaseR2dbc)
    testImplementation("dev.slne.surf.nats:surf-nats-standalone:$surfNatsVersion")
    testImplementation("dev.slne.surf.nats:surf-nats-test-common:$surfNatsVersion")
    testImplementation(libs.jnats)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.nats)
    testImplementation(libs.mariadb.jdbc)
    testImplementation(libs.postgresql.jdbc)
    testRuntimeOnly("dev.slne.surf.api:surf-api-standalone:+")
}

/*
 * surf-database and surf-nats are shaded jars that reference configurate as `dev.slne.surf.api.libs.configurate`, the
 * package a microservice's shadow jar relocates it to, while the published surf API artifacts use the original
 * package. The tests therefore run with the surf API and configurate bundled into one jar; the surf gradle plugin
 * applies the configurate relocation to every ShadowJar task.
 */
val surfRuntimeGroups = setOf("dev.slne.surf.api", "dev.slne.forks.configurate")

fun testRuntimeArtifacts(surfRuntime: Boolean) = configurations.testRuntimeClasspath.map { configuration ->
    configuration.incoming.artifactView {
        componentFilter { id ->
            val isSurfRuntime = id is ModuleComponentIdentifier && id.group in surfRuntimeGroups
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
