plugins {
    `core-convention`
}

description = "surf-api-velocity-shared-bean-test"

/**
 * One artifact the test downloads: its name on disk, where it comes from and what it has to be.
 */
data class PinnedDownload(
    val id: String,
    val fileName: String,
    val url: String,
    val sha256: String,
) {

    /**
     * The three lines describing this artifact in the test's input file.
     */
    fun asProperties() = """
        download.$id.file=$fileName
        download.$id.url=$url
        download.$id.sha256=$sha256
    """.trimIndent()
}

/**
 * The release part of a Modrinth version name such as `v5.5.71-bukkit`.
 */
fun modrinthRelease(version: String) = version.removePrefix("v").substringBefore('-')

/**
 * Pins every artifact the test downloads, with the checksum of that exact version.
 *
 * The three plugin versions come from `gradle/libs.versions.toml`, so the test runs against the
 * versions the repository declares. None of them is obtainable as a usable Maven artifact: the
 * proxy itself is distributed only through PaperMC's download service, and the three plugins are
 * published to Maven as thin API jars rather than as the shaded plugins a proxy loads. So each one
 * is a pinned URL plus the checksum of that version, and bumping a version means updating the
 * checksum with it -- a mismatch makes the test skip and say so rather than run against something
 * unknown.
 */
val pinnedDownloads = listOf(
    // Velocity 4.3.1 build 45, the release `velocity-api` in the version catalog names. The download
    // path is keyed by the jar's own sha256; the build index is
    // https://fill.papermc.io/v3/projects/velocity/versions/4.3.1/builds.
    PinnedDownload(
        id = "velocity",
        fileName = "velocity-4.3.1-45.jar",
        url = "https://fill-data.papermc.io/v1/objects/" +
                "0844bcabdcdb4f5b875906fd4c3365bc1bb76918e42bc595ed88ad195258dd75/" +
                "velocity-4.3.1-45.jar",
        sha256 = "0844bcabdcdb4f5b875906fd4c3365bc1bb76918e42bc595ed88ad195258dd75",
    ),
    // CommandAPI's Velocity plugin jar, from its GitHub release.
    libs.versions.commandapiplugin.get().let { version ->
        PinnedDownload(
            id = "commandapi",
            fileName = "CommandAPI-$version-Velocity.jar",
            url = "https://github.com/CommandAPI/CommandAPI/releases/download/" +
                    "$version/CommandAPI-$version-Velocity.jar",
            sha256 = "bdf9372c6651eb9ca0e3b5dcd4f4a10c90ce1914e3ed3ebdf7d4a2b420c8ad3b",
        )
    },
    // packetevents' shaded Velocity plugin jar, from its GitHub release.
    libs.versions.packetevents.plugin.get().let { version ->
        PinnedDownload(
            id = "packetevents",
            fileName = "packetevents-velocity-$version.jar",
            url = "https://github.com/retrooper/packetevents/releases/download/" +
                    "v$version/packetevents-velocity-$version.jar",
            sha256 = "0772f64b51aaf79132c76bb1503707ea0d35433713d5312e67401eaa599d271b",
        )
    },
    // LuckPerms' Velocity plugin jar, from Modrinth's permanent per-version CDN path. LuckPerms
    // downloads its own libraries into `plugins/luckperms/libs` the first time it starts, which is
    // why the assembled proxy directory is kept between runs.
    modrinthRelease(libs.versions.luckpermsplugin.bukkit.get()).let { release ->
        PinnedDownload(
            id = "luckperms",
            fileName = "LuckPerms-Velocity-$release.jar",
            url = "https://cdn.modrinth.com/data/Vebnzrzj/versions/tamnmXad/" +
                    "LuckPerms-Velocity-$release.jar",
            sha256 = "a3ac92ef4e29fd87a4d706dc92445fd946f1caf1d33526ca18422a62554fa18a",
        )
    },
)

/**
 * The boot test's own source set.
 *
 * Deliberately not `test`: this test downloads a proxy and starts it, so it must never be reached
 * by `check`, by `test`, or by anything CI runs. A source set of its own keeps that true by
 * construction rather than by remembering to exclude it.
 */
val velocityTest: SourceSet by sourceSets.creating

/**
 * The plugin jars the assembled proxy runs.
 *
 * `isTransitive = false` because what is wanted is the three artifacts themselves and not their
 * dependency closures: surf-api's plugin jar already carries everything it needs, and the two test
 * plugins are compiled against surf-api rather than bundling it.
 */
val proxyPlugins by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    // Declared because `kotlin.stdlib.default.dependency=false` holds for the whole build.
    velocityTest.implementationConfigurationName(libs.kotlin.stdlib)

    // The JUnit api directly, rather than `kotlin("test")`: the Kotlin plugin picks that
    // artifact's framework variant from the `test` task, and this test has a task of its own.
    velocityTest.implementationConfigurationName(libs.junit.jupiter)
    velocityTest.runtimeOnlyConfigurationName(libs.junit.platform.launcher)

    // For the marker strings the plugins write, so the test and the plugins cannot drift. They are
    // `const val`, so nothing of these modules is needed at runtime.
    velocityTest.compileOnlyConfigurationName(projects.surfApiVelocitySharedBeanConsumer)
    velocityTest.compileOnlyConfigurationName(projects.surfApiVelocitySharedBeanProvider)

    // The shadow jar is surf-api's actual Velocity plugin; the other two have nothing to shade.
    proxyPlugins(project(path = ":surf-api-velocity-impl", configuration = "shadowRuntimeElements"))
    proxyPlugins(
        project(path = ":surf-api-velocity-shared-bean-provider", configuration = "runtimeElements")
    )
    proxyPlugins(
        project(path = ":surf-api-velocity-shared-bean-consumer", configuration = "runtimeElements")
    )
}

/**
 * Where the test caches what it downloads and assembles the proxy.
 *
 * Under the build directory, so `clean` discards it while a second run reuses the proxy jar and
 * the libraries LuckPerms fetches on its first start.
 */
val proxyWorkDir = layout.buildDirectory.dir("velocity-shared-bean-test")

/**
 * Hands [pinnedDownloads] and the plugin jars to the test as a properties file.
 *
 * A file rather than system properties: these are URLs and checksums for a list whose length is a
 * build concern, and a file keeps the test reading one input instead of guessing property names.
 */
val writeBootTestInputs = tasks.register("writeVelocitySharedBeanBootTestInputs") {
    group = "verification"
    description = "Writes the pinned downloads and plugin jars the Velocity shared-bean boot test needs."

    val inputsFile = proxyWorkDir.map { it.file("inputs.properties") }
    val pluginJars = proxyPlugins.incoming.files
    val downloads = pinnedDownloads.joinToString("\n", postfix = "\n") { it.asProperties() }

    inputs.property("downloads", downloads)
    inputs.files(pluginJars).withPropertyName("pluginJars")
    outputs.file(inputsFile)

    doLast {
        val file = inputsFile.get().asFile
        // A properties value may hold `:` and `/` unescaped but not `\`, and the path separator is
        // `;` on the one platform whose paths contain a `:` at all.
        val jars = pluginJars.joinToString(File.pathSeparator) {
            it.absolutePath.replace("\\", "\\\\")
        }

        file.parentFile.mkdirs()
        file.writeText(downloads + "pluginJars=" + jars + "\n")
    }
}

/**
 * JVM arguments for the test JVM and for the proxy process it starts, from the Gradle property
 * `surf.bootTest.jvmArgs`, separated by whitespace.
 *
 * Both JVMs download over HTTPS and need the same settings, for example
 * `-Djavax.net.ssl.trustStoreType=Windows-ROOT` behind a TLS-intercepting proxy.
 */
val bootTestJvmArgs = providers.gradleProperty("surf.bootTest.jvmArgs")
    .map { it.trim().split(Regex("\\s+")).filter(String::isNotEmpty) }
    .orElse(emptyList())

/**
 * Starts a real Velocity proxy with surf-api and the two test plugins, and asserts on its log.
 *
 * Not wired into `check` and not wired into `test`: run it by name.
 */
tasks.register<Test>("velocitySharedBeanBootTest") {
    group = "verification"
    description = "Boots a real Velocity proxy and proves a @PluginApi bean crosses two plugins."

    testClassesDirs = velocityTest.output.classesDirs
    classpath = velocityTest.runtimeClasspath

    dependsOn(writeBootTestInputs)

    jvmArgs(bootTestJvmArgs.get())
    systemProperty("surf.velocityBootTest.jvmArgs", bootTestJvmArgs.get().joinToString(" "))

    systemProperty(
        "surf.velocityBootTest.inputs",
        proxyWorkDir.get().file("inputs.properties").asFile.absolutePath,
    )
    systemProperty("surf.velocityBootTest.workDir", proxyWorkDir.get().asFile.absolutePath)
    systemProperty(
        "surf.velocityBootTest.java",
        javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile.absolutePath,
    )

    // Booting a proxy is never up to date in any sense a person asking for this task means.
    outputs.upToDateWhen { false }

    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
