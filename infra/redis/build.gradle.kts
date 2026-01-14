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

plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
}

allprojects {
    group = "dev.slne.surf"
    version = findProperty("version") as String
}

subprojects {
    tasks.withType<ShadowJar>().configureEach {
        val base = "dev.slne.surf.redis.libs."

        relocate("com.esotericsoftware", base + "kryo")
        relocate("io.netty", base + "netty")
        relocate("io.reactivex", base + "reactivex")
        relocate("javax.cache", base + "javax.cache")
        relocate("jodd", base + "jodd")
        relocate("net.bytebuddy", base + "bytebuddy")
        relocate("org.objenesis", base + "objenesis")
        relocate("org.redisson", base + "redisson")
        relocate("org.yaml", base + "yaml")
    }

    afterEvaluate {
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

apiValidation {
    nonPublicMarkers.add("dev.slne.surf.redis.util.InternalRedisAPI")
    apiDumpDirectory = "api"
    ignoredProjects.addAll(listOf("surf-redis-core", "surf-redis-paper", "surf-redis-velocity"))
}