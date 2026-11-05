package dev.slne.surf.microservice.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs a `main` class of the test class path in a new JVM, for behavior that is fixed once per JVM or ends it.
 *
 * Relies on `java.class.path` holding the full test class path, as it does under Gradle.
 */
internal object ProbeJvm {

    /**
     * Runs [mainClass] with [args] in a temporary working directory and returns once the JVM exited.
     *
     * @throws AssertionError if the JVM still runs after [timeout]; it is killed first
     */
    fun run(mainClass: Class<*>, args: List<String> = emptyList(), timeout: Duration = 60.seconds): Result {
        val directory = Files.createTempDirectory("probe-jvm")
        val argumentFile = directory.resolve("java.args")
        val outputFile = directory.resolve("out.txt")
        val errorFile = directory.resolve("err.txt")
        try {
            val classPath = System.getProperty("java.class.path").replace("\\", "\\\\")
            argumentFile.writeText("-cp \"$classPath\"")
            val process = ProcessBuilder(listOf(javaExecutable(), "@$argumentFile", mainClass.name) + args)
                .directory(directory.toFile())
                .redirectOutput(outputFile.toFile())
                .redirectError(errorFile.toFile())
                .start()

            if (!process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor()
                throw AssertionError("The probe JVM did not exit within $timeout.\n${errorFile.readText()}")
            }
            return Result(process.exitValue(), outputFile.readText(), errorFile.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun javaExecutable() = Path.of(System.getProperty("java.home"), "bin", "java").toString()

    data class Result(val exitCode: Int, val output: String, val errorOutput: String)
}
