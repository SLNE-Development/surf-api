import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.20" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    configurations.all {
        if (name == "compileOnly") {
            return@all
        }

        dependencies.remove(project.dependencies.gradleApi())
        dependencies.remove(project.dependencies.gradleTestKit())
    }
}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                optIn.add("dev.slne.surf.api.shared.api.util.InternalSurfApi")
                freeCompilerArgs.add("-Xcontext-parameters")
            }
        }
    }
}