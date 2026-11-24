import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
    id("dev.slne.surf.api.gradle.core") apply false
}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinJvmExtension>()?.apply {
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
