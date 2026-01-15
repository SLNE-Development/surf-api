import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import kotlinx.validation.KotlinApiBuildTask

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
//    alias(libs.plugins.dokka)
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    afterEvaluate {
        tasks {
            withType<KotlinApiBuildTask> {
                inputJar.value(shadowJar.flatMap { it.archiveFile })
            }
        }
    }

    configurations.all {
        if (name == "compileOnly") {
            return@all
        }

        dependencies.remove(project.dependencies.gradleApi())
        dependencies.remove(project.dependencies.gradleTestKit())
    }

//    if (subprojects.isEmpty()) {
//        apply(plugin = rootProject.libs.plugins.dokka.get().pluginId)
//    }
}

val ci = System.getenv("CI")?.toBoolean() == true
apiValidation {
    nonPublicMarkers.add("dev.slne.surf.surfapi.core.api.util.InternalSurfApi")
    ignoredProjects.addAll(
        listOf(
            "surf-api-core-server",
            "surf-api-bukkit-server",
            "surf-api-hytale-server",
            "surf-api-velocity-server",
            "surf-api-standalone",
            "surf-api-gradle-plugin",
            "surf-api-processor"
        )
    )

    if (!ci) {
        ignoredProjects.addAll(
            listOf(
                "surf-api-bukkit-plugin-test",
                "surf-api-modern-generator"
            )
        )
    }
}

//dependencies {
//    dokka(project(":surf-api-core:surf-api-core-api"))
//    dokka(project(":surf-api-bukkit:surf-api-bukkit-api"))
//    dokka(project(":surf-api-velocity:surf-api-velocity-api"))
//}
//
//dokka {
//    dokkaSourceSets.configureEach {
//        documentedVisibilities = setOf(VisibilityModifier.Public, VisibilityModifier.Protected)
//    }
//}
