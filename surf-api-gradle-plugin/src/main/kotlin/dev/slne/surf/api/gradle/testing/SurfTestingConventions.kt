package dev.slne.surf.api.gradle.testing

import dev.slne.surf.api.gradle.generated.Constants
import kotlinx.benchmark.gradle.BenchmarksExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

private const val BENCHMARK_SOURCE_SET_NAME = "benchmark"
private const val JMH_STATE_ANNOTATION = "org.openjdk.jmh.annotations.State"

internal fun Project.configureSurfTestingConventions() {
    val sourceSets = extensions.getByType<JavaPluginExtension>().sourceSets
    val main = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get()
    val test = sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME).get()

    configurations.named(test.compileOnlyConfigurationName) {
        extendsFrom(configurations.getByName(main.compileOnlyConfigurationName))
    }
    configurations.named(test.runtimeOnlyConfigurationName) {
        extendsFrom(configurations.getByName(main.compileOnlyConfigurationName))
    }

    dependencies {
        add(test.implementationConfigurationName, Constants.KOTLIN_TEST_JUNIT5)
        add(test.implementationConfigurationName, Constants.JUNIT_JUPITER)
        add(test.implementationConfigurationName, Constants.COROUTINES_TEST)
        add(test.implementationConfigurationName, Constants.MOCKK)
        add(test.implementationConfigurationName, Constants.LINCHECK)
        add(test.runtimeOnlyConfigurationName, Constants.JUNIT_PLATFORM_LAUNCHER)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    val benchmark = sourceSets.maybeCreate(BENCHMARK_SOURCE_SET_NAME).apply {
        compileClasspath += main.output
        runtimeClasspath += output + main.output
    }

    configurations.named(benchmark.implementationConfigurationName) {
        extendsFrom(configurations.getByName(main.implementationConfigurationName))
    }
    configurations.named(benchmark.compileOnlyConfigurationName) {
        extendsFrom(configurations.getByName(main.compileOnlyConfigurationName))
    }
    configurations.named(benchmark.runtimeOnlyConfigurationName) {
        extendsFrom(
            configurations.getByName(main.runtimeOnlyConfigurationName),
            configurations.getByName(main.compileOnlyConfigurationName)
        )
    }

    dependencies {
        add(benchmark.implementationConfigurationName, Constants.KOTLINX_BENCHMARK_RUNTIME)
    }

    extensions.configure<KotlinJvmProjectExtension> {
        target.compilations.getByName(BENCHMARK_SOURCE_SET_NAME)
            .associateWith(target.compilations.getByName(SourceSet.MAIN_SOURCE_SET_NAME))
    }

    extensions.configure<AllOpenExtension> {
        annotation(JMH_STATE_ANNOTATION)
    }

    extensions.configure<BenchmarksExtension> {
        if (targets.findByName(BENCHMARK_SOURCE_SET_NAME) == null) {
            targets.register(BENCHMARK_SOURCE_SET_NAME)
        }
    }
}
