import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
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

val ci = System.getenv("CI")?.toBoolean() == true
//apiValidation {
//    nonPublicMarkers.add("dev.slne.surf.surfapi.core.api.util.InternalSurfApi")
//    ignoredProjects.addAll(
//        listOf(
//            "surf-api-core-server",
//            "surf-api-bukkit-server",
//            "surf-api-hytale-server",
//            "surf-api-velocity-server",
//            "surf-api-standalone",
//            "surf-api-gradle-plugin",
//            "surf-api-processor"
//        )
//    )
//
//    if (!ci) {
//        ignoredProjects.addAll(
//            listOf(
//                "surf-api-bukkit-plugin-test",
//                "surf-api-modern-generator"
//            )
//        )
//    }
//}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                optIn.add("dev.slne.surf.surfapi.core.api.util.InternalSurfApi")
            }
        }
    }
}