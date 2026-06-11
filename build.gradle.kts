import dev.slne.surf.api.generator.nms.NmsVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21" apply false
    id("dev.slne.surf.api.generator.nms-module-generator")
}

nmsGenerator {
    referenceVersion = NmsVersion.V1_21_11
    targetVersion = NmsVersion.V26_1
}


allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.jsinco.dev/releases/")
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