package dev.slne.surf.api.gradle.platform.common.testing

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal object SurfTestingConfigurer {
    internal const val TEST_IMPLEMENTATION = "testImplementation"
    internal const val TEST_RUNTIME_ONLY = "testRuntimeOnly"

    fun configure(project: Project, testing: SurfTestingExtension) = with(project) {
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
                "org.junit.jupiter:junit-jupiter:${testing.junitVersion.get()}"
            )
            add(
                TEST_RUNTIME_ONLY,
                "org.junit.platform:junit-platform-launcher:${testing.junitPlatformLauncherVersion.get()}"
            )
            add(TEST_IMPLEMENTATION, "org.jetbrains.kotlin:kotlin-test-junit5")
            add(
                TEST_IMPLEMENTATION,
                "org.jetbrains.kotlinx:kotlinx-coroutines-test:${testing.coroutinesTestVersion.get()}"
            )

            if (testing.mockk.get()) {
                add(TEST_IMPLEMENTATION, "io.mockk:mockk:${testing.mockkVersion.get()}")
            }
            if (testing.lincheck.get()) {
                add(
                    TEST_IMPLEMENTATION,
                    "org.jetbrains.lincheck:lincheck:${testing.lincheckVersion.get()}"
                )
            }
        }
    }
}
