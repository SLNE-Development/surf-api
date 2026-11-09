plugins {
    `core-convention`
}

description = "surf-api-paper-shared-bean-test"

/** One artifact the test downloads: its name on disk, where it comes from and what it has to be. */
data class PinnedDownload(
    val id: String,
    val fileName: String,
    val url: String,
    val sha256: String,
) {

    /** The three lines describing this artifact in the test's input file. */
    fun asProperties() = """
        download.$id.file=$fileName
        download.$id.url=$url
        download.$id.sha256=$sha256
    """.trimIndent()
}

/** The release part of a Modrinth version name such as `v5.5.71-bukkit`. */
fun modrinthRelease(version: String) = version.removePrefix("v").substringBefore('-')

/**
 * Pins every artifact the test downloads, with the checksum of that exact version.
 *
 * The three plugin versions come from `gradle/libs.versions.toml`, so the test runs against the
 * versions the repository declares. None of them is obtainable as a usable Maven artifact:
 * `dev.jorel:commandapi-paper-plugin` stops at 12.0.0 on Maven Central, and that build refuses to
 * start on Paper 26.3; `com.github.retrooper:packetevents-spigot` is published, but as a thin jar
 * whose manifest `Class-Path` names sibling jars rather than as the shaded plugin a server loads;
 * and LuckPerms publishes only its api to Maven. So each one is a pinned URL plus the checksum of
 * that version, and bumping a version means updating the checksum with it -- a mismatch makes the
 * test skip and say so rather than run against something unknown.
 */
val pinnedDownloads = listOf(
    // Paper 26.3 build 245. The download path is keyed by the jar's own sha256; the build index is
    // https://fill.papermc.io/v3/projects/paper/versions/26.3/builds?channel=STABLE.
    PinnedDownload(
        id = "paper",
        fileName = "paper-26.3-245.jar",
        url = "https://fill-data.papermc.io/v1/objects/" +
                "192d277ea1ce29b2df3933dd488521d4555cc82be864ea8df811fe536e8aed43/" +
                "paper-26.3-245.jar",
        sha256 = "192d277ea1ce29b2df3933dd488521d4555cc82be864ea8df811fe536e8aed43",
    ),
    // CommandAPI's Paper plugin jar, from its GitHub release.
    libs.versions.commandapiplugin.get().let { version ->
        PinnedDownload(
            id = "commandapi",
            fileName = "CommandAPI-$version-Paper.jar",
            url = "https://github.com/CommandAPI/CommandAPI/releases/download/" +
                    "$version/CommandAPI-$version-Paper.jar",
            sha256 = "ab48e55fd06264dd239cf015b501936cb50eb6c38d967fbffca6c91a48064049",
        )
    },
    // packetevents' shaded Spigot plugin jar, from its GitHub release.
    libs.versions.packetevents.plugin.get().let { version ->
        PinnedDownload(
            id = "packetevents",
            fileName = "packetevents-spigot-$version.jar",
            url = "https://github.com/retrooper/packetevents/releases/download/" +
                    "v$version/packetevents-spigot-$version.jar",
            sha256 = "060087c58ec268eae7dd0eb1f17ac3bea1eaadda1693414141ff64e506091bd7",
        )
    },
    // LuckPerms' Bukkit plugin jar, from Modrinth's permanent per-version CDN path. LuckPerms
    // downloads its own libraries into `plugins/LuckPerms/libs` the first time it starts, which is
    // why the assembled server directory is kept between runs.
    modrinthRelease(libs.versions.luckpermsplugin.bukkit.get()).let { release ->
        PinnedDownload(
            id = "luckperms",
            fileName = "LuckPerms-Bukkit-$release.jar",
            url = "https://cdn.modrinth.com/data/Vebnzrzj/versions/b0mk8uS6/" +
                    "LuckPerms-Bukkit-$release.jar",
            sha256 = "49cecb66fa1fd22a133039a490e9c1e5095a238e7cd66eb9d2a16fe6c897550d",
        )
    },
)

/**
 * The boot test's own source set.
 *
 * Deliberately not `test`: this test downloads a Minecraft server and starts it, so it must never
 * be reached by `check`, by `test`, or by anything CI runs. A source set of its own keeps that true
 * by construction rather than by remembering to exclude it.
 */
val paperTest: SourceSet by sourceSets.creating

/**
 * The plugin jars the assembled server runs.
 *
 * `isTransitive = false` because what is wanted is the three artifacts themselves and not their
 * dependency closures: surf-api's plugin jar already carries everything it needs, and the two test
 * plugins are compiled against surf-api rather than bundling it.
 */
val serverPlugins by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    // Declared because `kotlin.stdlib.default.dependency=false` holds for the whole build.
    paperTest.implementationConfigurationName(libs.kotlin.stdlib)

    // The JUnit api directly, rather than `kotlin("test")`: the Kotlin plugin picks that
    // artifact's framework variant from the `test` task, and this test has a task of its own.
    paperTest.implementationConfigurationName(libs.junit.jupiter)
    paperTest.runtimeOnlyConfigurationName(libs.junit.platform.launcher)

    // For the marker strings the consuming plugin writes, so the test and the plugin cannot drift.
    // They are `const val`, so nothing of these modules is needed at runtime.
    paperTest.compileOnlyConfigurationName(projects.surfApiPaperSharedBeanConsumer)
    paperTest.compileOnlyConfigurationName(projects.surfApiPaperSharedBeanProvider)

    // The shadow jar is surf-api's actual Paper plugin; the other two have nothing to shade.
    serverPlugins(project(path = ":surf-api-paper-impl", configuration = "shadowRuntimeElements"))
    serverPlugins(
        project(path = ":surf-api-paper-shared-bean-provider", configuration = "runtimeElements")
    )
    serverPlugins(
        project(path = ":surf-api-paper-shared-bean-consumer", configuration = "runtimeElements")
    )
}

/**
 * Where the test caches what it downloads and assembles the server.
 *
 * Under the build directory, so `clean` discards it while a second run reuses the server jar,
 * Paper's own extracted libraries and the libraries LuckPerms fetches on its first start.
 */
val serverWorkDir = layout.buildDirectory.dir("paper-shared-bean-test")

/**
 * Hands [pinnedDownloads] and the plugin jars to the test as a properties file.
 *
 * A file rather than system properties: these are URLs and checksums for a list whose length is a
 * build concern, and a file keeps the test reading one input instead of guessing property names.
 */
val writeBootTestInputs = tasks.register("writeSharedBeanBootTestInputs") {
    group = "verification"
    description = "Writes the pinned downloads and plugin jars the shared-bean boot test needs."

    val inputsFile = serverWorkDir.map { it.file("inputs.properties") }
    val pluginJars = serverPlugins.incoming.files
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
 * Starts a real Paper server with surf-api and the two test plugins, and asserts on its log.
 *
 * Not wired into `check` and not wired into `test`: run it by name.
 */
tasks.register<Test>("paperSharedBeanBootTest") {
    group = "verification"
    description = "Boots a real Paper server and proves a Scope.SHARED bean crosses two plugins."

    testClassesDirs = paperTest.output.classesDirs
    classpath = paperTest.runtimeClasspath

    dependsOn(writeBootTestInputs)

    systemProperty(
        "surf.sharedBeanBootTest.inputs",
        serverWorkDir.get().file("inputs.properties").asFile.absolutePath,
    )
    systemProperty("surf.sharedBeanBootTest.workDir", serverWorkDir.get().asFile.absolutePath)
    systemProperty(
        "surf.sharedBeanBootTest.java",
        javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile.absolutePath,
    )

    // Booting a server is never up to date in any sense a person asking for this task means.
    outputs.upToDateWhen { false }

    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
