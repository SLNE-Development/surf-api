package dev.slne.surf.api.gradle.platform

import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.microservice.gradle.testkit.TestBuild
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Applies the plugin platforms through TestKit, offline, and reads the module dependencies declared
 * on `testRuntimeOnly` without resolving them.
 */
class PluginTestRuntimeBuildTest {

    private fun TestBuild.testRuntimeOnly(): Set<String> {
        val output = run("printTestRuntimeOnly", "--quiet").output

        return output.lineSequence()
            .filter { line -> line.startsWith(DEPENDENCY_PREFIX) }
            .map { line -> line.removePrefix(DEPENDENCY_PREFIX) }
            .toSet()
    }

    private fun project(
        info: TestInfo,
        plugin: String,
        extension: String,
        configure: String = "",
    ): TestBuild {
        return TestBuild.create(info.displayName, withSurfApi = true).apply {
            file("gradle.properties", "kotlin.stdlib.default.dependency=false")
            script(
                """
                plugins { id '$plugin' }
                $extension {
                    $configure
                }
                tasks.register('printTestRuntimeOnly') {
                    def dependencies = configurations.testRuntimeOnly.dependencies
                        .withType(ExternalModuleDependency)
                    doLast {
                        dependencies.each { dependency ->
                            def notation = [dependency.group, dependency.name, dependency.version]
                            println('$DEPENDENCY_PREFIX' + notation.join(':'))
                        }
                    }
                }
                """,
            )
        }
    }

    private fun assertRuntimeLibraries(declared: Set<String>, platformLibraries: List<String>) {
        val expected = Constants.COMMON_RUNTIME_TEST_LIBRARIES + platformLibraries

        assertTrue(declared.containsAll(expected), "missing: ${expected - declared}")
    }

    @Test
    fun `paper plugin tests get the surf-api runtime libraries`(info: TestInfo) {
        val declared = project(info, "dev.slne.surf.api.gradle.paper-plugin", "surfPaperPluginApi")
            .testRuntimeOnly()

        assertRuntimeLibraries(declared, Constants.PAPER_RUNTIME_TEST_LIBRARIES)
        assertFalse(declared.any { it.startsWith("net.kyori:adventure-api:") })
        assertFalse(declared.any { it.startsWith("com.google.guava:guava:") })
    }

    @Test
    fun `velocity plugin tests get the surf-api runtime libraries`(info: TestInfo) {
        val declared = project(info, "dev.slne.surf.api.gradle.velocity", "surfVelocityApi")
            .testRuntimeOnly()

        assertRuntimeLibraries(declared, Constants.VELOCITY_RUNTIME_TEST_LIBRARIES)
        assertFalse(declared.any { it.startsWith("net.kyori:adventure-api:") })
    }

    @Test
    fun `minestom plugin tests get the surf-api runtime libraries`(info: TestInfo) {
        val declared = project(info, "dev.slne.surf.api.gradle.minestom", "surfMinestomApi")
            .testRuntimeOnly()

        assertRuntimeLibraries(declared, Constants.MINESTOM_RUNTIME_TEST_LIBRARIES)
    }

    @Test
    fun `plugin without testing gets no runtime libraries`(info: TestInfo) {
        val declared = project(
            info,
            plugin = "dev.slne.surf.api.gradle.paper-plugin",
            extension = "surfPaperPluginApi",
            configure = "testing { enabled = false }",
        ).testRuntimeOnly()

        assertEquals(emptySet(), declared)
    }

    private companion object {
        const val DEPENDENCY_PREFIX = "testRuntimeOnly="
    }
}
