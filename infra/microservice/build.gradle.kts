import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
    dependencies {
        classpath("dev.slne.surf:surf-api-gradle-plugin:26+")
    }
}

allprojects {
    group = "dev.slne.surf.microservice"
    version = findProperty("version") as String
}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                optIn.add("dev.slne.surf.microservice.api.common.util.InternalMicroserviceApi")
            }
        }

        extensions.findByType<PublishingExtension>()?.apply {
            repositories {
                slneReleases()
            }
        }
    }
}