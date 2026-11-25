package dev.slne.surf.api.gradle.platform.common.testing

import dev.slne.surf.api.gradle.generated.Constants
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal object SurfTestingConfigurer {
    internal const val TEST_IMPLEMENTATION = "testImplementation"
    internal const val TEST_RUNTIME_ONLY = "testRuntimeOnly"

    /**
     * Adds the libraries the surf-api plugin runtime ships to `testRuntimeOnly`.
     *
     * The surf-api api modules declare them `compileOnlyApi`, so a plugin compiles against them but
     * a test that runs without a server would miss them.
     *
     * @param platformLibraries the libraries only this platform's runtime ships
     */
    fun addSurfRuntimeLibraries(project: Project, platformLibraries: List<String>) {
        for (notation in Constants.COMMON_RUNTIME_TEST_LIBRARIES + platformLibraries) {
            project.dependencies.add(TEST_RUNTIME_ONLY, notation)
        }
    }

    fun configure(project: Project, testing: SurfTestingExtension) {
        with(project) {
            tasks.withType<Test>().configureEach {
                if (testing.useJUnitPlatform.get()) {
                    useJUnitPlatform()
                }
                // Lincheck attaches its bytecode-instrumentation agent dynamically at runtime
                jvmArgs("-XX:+EnableDynamicAgentLoading")
            }

            dependencies {
                add(
                    TEST_IMPLEMENTATION,
                    "org.junit.jupiter:junit-jupiter:${testing.junitVersion.get()}",
                )
                add(
                    TEST_RUNTIME_ONLY,
                    "org.junit.platform:junit-platform-launcher:${testing.junitPlatformLauncherVersion.get()}",
                )
                add(TEST_IMPLEMENTATION, "org.jetbrains.kotlin:kotlin-test-junit5")
                add(
                    TEST_IMPLEMENTATION,
                    "org.jetbrains.kotlinx:kotlinx-coroutines-test:${testing.coroutinesTestVersion.get()}",
                )

                if (testing.mockk.get()) {
                    add(TEST_IMPLEMENTATION, "io.mockk:mockk:${testing.mockkVersion.get()}")
                }

                if (testing.lincheck.get()) {
                    add(
                        TEST_IMPLEMENTATION,
                        "org.jetbrains.lincheck:lincheck:${testing.lincheckVersion.get()}",
                    )
                }
            }
        }
    }
}
