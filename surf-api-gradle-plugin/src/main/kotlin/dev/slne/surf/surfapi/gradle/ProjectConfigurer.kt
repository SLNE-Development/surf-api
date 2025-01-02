package dev.slne.surf.surfapi.gradle

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal object ProjectConfigurer {

    fun Project.configureKotlin() = configure<KotlinJvmProjectExtension> {
        jvmToolchain(Constants.JAVA_VERSION)
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
        }
    }

    fun configureCore(project: Project, configuration: Core) {
    }
}