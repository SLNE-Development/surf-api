import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    id("core-convention")
}

/**
 * Deliberately not `test`: these tests download a Minecraft server and start it, so `check`,
 * `test` and CI must never reach them.
 */
val paperTest = sourceSets.create(PAPER_SERVER_TEST_SOURCE_SET)

/**
 * `isTransitive = false` because the test installs the artifacts themselves: surf-api's plugin jar
 * already carries what it needs, and the test plugins compile against surf-api instead of bundling
 * it.
 */
val serverPlugins = configurations.create(PAPER_SERVER_PLUGINS) {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    paperTest.implementationConfigurationName(libs.kotlin.stdlib)
    // The JUnit api directly, rather than `kotlin("test")`: the Kotlin plugin picks that artifact's
    // framework variant from the `test` task, and these tests have tasks of their own.
    paperTest.implementationConfigurationName(libs.junit.jupiter)
    paperTest.implementationConfigurationName(project(":surf-api-paper-server-harness"))
    paperTest.runtimeOnlyConfigurationName(libs.junit.platform.launcher)

    serverPlugins(project(path = ":surf-api-paper-impl", configuration = "shadowRuntimeElements"))
}

/**
 * A file rather than system properties: the download list's length is a build concern, and a file
 * keeps the test reading one input instead of guessing property names.
 */
tasks.register(WRITE_PAPER_SERVER_TEST_INPUTS) {
    group = "verification"
    description = "Writes the pinned downloads and plugin jars a Paper server test needs."

    val inputsFile = paperServerTestWorkDir().resolve(PAPER_SERVER_TEST_INPUTS_FILE)
    val pluginJars = serverPlugins.incoming.files
    val downloads = paperServerDownloads(libs).joinToString("\n", postfix = "\n") { download ->
        download.asProperties()
    }

    inputs.property("downloads", downloads)
    inputs.files(pluginJars).withPropertyName("pluginJars")
    outputs.file(inputsFile)

    doLast {
        // A properties value may hold `:` and `/` unescaped but not `\`, and the path separator is
        // `;` on the one platform whose paths contain a `:` at all.
        val jars = pluginJars.joinToString(File.pathSeparator) { jar ->
            jar.absolutePath.replace("\\", "\\\\")
        }

        inputsFile.parentFile.mkdirs()
        inputsFile.writeText(downloads + "pluginJars=" + jars + "\n")
    }
}
