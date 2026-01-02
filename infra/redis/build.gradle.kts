import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
    dependencies {
        classpath("dev.slne.surf:surf-api-gradle-plugin:1.21.11+")
    }
}

allprojects {
    group = "dev.slne.surf"
    version = findProperty("version") as String
}

subprojects {
    afterEvaluate {
        tasks.withType<ShadowJar> {
            relocationPrefix = "dev.slne.surf.redis.libs"
            enableAutoRelocation = true
        }

        configure<ShadowExtension> {
            addShadowVariantIntoJavaComponent = false
        }

        configure<KotlinJvmExtension> {
            compilerOptions {
                optIn.add("dev.slne.surf.redis.util.InternalRedisAPI")
            }
        }
    }
}