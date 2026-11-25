plugins {
    `core-convention`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinpoet)
}

tasks.register<JavaExec>("generateRegistryKeys") {
    group = "generation"
    description = "Regenerates the registry key objects of surf-api-registry-keys for mcVersion."

    mainClass = "dev.slne.surf.api.generator.registry.MainKt"
    classpath = sourceSets.main.get().runtimeClasspath
    args(
        providers.gradleProperty("mcVersion").get(),
        // Server jar, bundler libraries and data generator output; ignored by Git.
        layout.buildDirectory.dir("minecraft").get().asFile.absolutePath,
        rootProject.file("foundation/registry-keys/src/main/kotlin").absolutePath,
    )
    // A trust store configured for Gradle, e.g. behind a TLS-inspecting proxy, also has to reach
    // the forked JVM that downloads from piston-meta.
    for (property in listOf("javax.net.ssl.trustStore", "javax.net.ssl.trustStoreType")) {
        providers.systemProperty(property).orNull?.let { systemProperty(property, it) }
    }
}
