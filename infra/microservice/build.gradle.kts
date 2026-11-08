import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/releases")
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
    }
}

allprojects {
    group = "dev.slne.surf.microservice"
    version = findProperty("version") as String
}

subprojects {
    // TODO(v3 release): remove once surf-database-r2dbc 2.4.0 and surf-nats 1.0.0 are published.
    repositories {
        mavenLocal {
            content {
                includeVersion("dev.slne.surf", "surf-database-r2dbc", "2.4.0")
                includeGroup("dev.slne.surf.nats")
            }
        }
    }

    afterEvaluate {
        val usesApi = project.name !in setOf("surf-microservice-processor", "surf-microservice-gradle-plugin")
        extensions.findByType<KotlinJvmExtension>()?.takeIf { usesApi }?.apply {
            compilerOptions {
                optIn.add("dev.slne.surf.microservice.api.InternalMicroserviceApi")
            }
        }

        extensions.findByType<PublishingExtension>()?.apply {
            repositories {
                slneReleases()
            }
        }
    }
}