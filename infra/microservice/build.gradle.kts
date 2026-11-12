import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
    // Puts the surf plugin classes on this script's classpath for the imports above.
    id("dev.slne.surf.api.gradle.core") apply false
}

subprojects {
    afterEvaluate {
        val usesApi = project.name !in setOf("surf-api-microservice-processor")
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