import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/public/") { name = "public" }
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
        classpath("dev.slne.surf.microservice:surf-microservice-gradle-plugin:+")
    }
}

allprojects {
    group = "dev.slne.surf.nats"
    version = findProperty("version") as String
}

subprojects {
    tasks.withType<ShadowJar>().configureEach {
        relocate("io.nats", "dev.slne.surf.nats.libs.io.nats")
    }

    afterEvaluate {
        extensions.findByType<ShadowExtension>()?.apply {
            addShadowVariantIntoJavaComponent = false
        }

        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                optIn.add("dev.slne.surf.nats.InternalNatsApi")
            }
        }
    }
}
