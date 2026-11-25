package dev.slne.surf.api.gradle.internalapi

import dev.slne.surf.api.gradle.internalapi.InternalApiFixtures.INTERNAL_LIBRARY_API
import dev.slne.surf.api.gradle.internalapi.InternalApiFixtures.INTERNAL_SURF_API
import dev.slne.surf.microservice.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.plan
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Runs `checkInternalApiUsage` through TestKit, offline. Nothing can be compiled against surf-api,
 * so the task reads the fixture classes compiled with these tests instead of the project's own.
 */
class CheckInternalApiUsageBuildTest {

    private fun project(info: TestInfo, extension: String = ""): TestBuild {
        return TestBuild.create(info.displayName, withSurfApi = true).apply {
            InternalApiFixtures.copyConsumerClasses(dir.resolve("consumer-classes"))
            val libraryClasses = InternalApiFixtures.testClasses.invariantSeparatorsPath
            file("gradle.properties", "kotlin.stdlib.default.dependency=false")
            script(
                """
                plugins { id 'dev.slne.surf.api.gradle.core' }
                surfCoreApi {
                    $extension
                }
                tasks.named('checkInternalApiUsage') {
                    classes.setFrom('consumer-classes')
                    classpath.setFrom('$libraryClasses')
                }
                """,
            )
        }
    }

    @Test
    fun `check runs the internal api check`(info: TestInfo) {
        val build = project(info)

        val plan = build.run("check", "--dry-run").plan()

        assertContains(plan, ":checkInternalApiUsage")
    }

    @Test
    fun `a reference to internal api fails the build`(info: TestInfo) {
        val build = project(info)

        val result = build.runAndFail("checkInternalApiUsage")

        assertContains(result.output, "CallsInternalApi.run references")
        assertContains(result.output, "(@InternalLibraryApi)")
        assertContains(result.output, "allowInternalApi(")
    }

    @Test
    fun `allowed markers pass the check`(info: TestInfo) {
        val build = project(info, "allowInternalApi '$INTERNAL_SURF_API', '$INTERNAL_LIBRARY_API'")

        val result = build.run("checkInternalApiUsage")

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkInternalApiUsage")?.outcome)
    }

    @Test
    fun `allowing one marker still reports the others`(info: TestInfo) {
        val build = project(info, "allowInternalApi '$INTERNAL_SURF_API'")

        val result = build.runAndFail("checkInternalApiUsage")

        assertContains(result.output, "CallsLibraryInternalApi.run references")
        assertFalse("CallsInternalApi.run references" in result.output)
    }
}
