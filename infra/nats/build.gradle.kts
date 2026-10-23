import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.util.slnePublic
import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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

// The Gradle plugin dev.slne.surf.nats is an included build: `publish` and `publishToMavenLocal` publish it along with
// the modules of this build.
for (publishTask in listOf("publish", "publishToMavenLocal")) {
    tasks.register(publishTask) {
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        description = "Publishes the included build surf-nats-gradle-plugin (plugin artifact and marker)."
        dependsOn(gradle.includedBuild("surf-nats-gradle-plugin").task(":$publishTask"))
    }
}

val surfNatsModules = subprojects.filter { it.subprojects.isEmpty() }

subprojects {
    tasks.withType<ShadowJar>().configureEach {
        relocate("io.nats", "dev.slne.surf.nats.libs.io.nats")
    }

    configurations.matching { !it.name.startsWith("surfNats") }.configureEach {
        resolutionStrategy.dependencySubstitution {
            surfNatsModules.forEach { surfNatsModule ->
                substitute(module("dev.slne.surf.nats:${surfNatsModule.name}"))
                    .using(project(surfNatsModule.path))
            }
        }
    }

    // Kotlin Multiplatform modules (api, core): shared toolchain, compiler options, dependency pattern (spec 12.2)
    // and publishing. Module scripts declare their targets and their own dependencies.
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "maven-publish")

        repositories {
            mavenCentral()
            slnePublic()
            maven("https://repo.papermc.io/repository/maven-public/")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(25)

            compilerOptions {
                freeCompilerArgs.addAll("-Xcontext-parameters", "-Xreturn-value-checker=check")
                optIn.addAll("dev.slne.surf.nats.InternalNatsApi", "dev.slne.surf.nats.NatsGeneratedApi")
            }

            targets.withType<KotlinJvmTarget>().configureEach {
                compilerOptions {
                    freeCompilerArgs.add("-Xjsr305=strict")
                    javaParameters = true
                }
            }

            sourceSets {
                commonMain.dependencies {
                    compileOnly(kotlin("stdlib"))
                    compileOnly(libs.kotlinx.serialization.core)
                    compileOnly(libs.kotlinx.coroutines.core)
                }
                jsMain.dependencies {
                    api(kotlin("stdlib"))
                    api(libs.kotlinx.serialization.core)
                    api(libs.kotlinx.coroutines.core)
                }
            }
        }

        extensions.configure<PublishingExtension> {
            repositories {
                slneReleases()
            }
        }
    }

    afterEvaluate {
        extensions.findByType<ShadowExtension>()?.apply {
            addShadowVariantIntoJavaComponent = false
        }

        val optInInternalApi = !project.path.startsWith(":surf-nats-test") &&
                !project.path.startsWith(":surf-nats-types")

        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                if (optInInternalApi) {
                    optIn.add("dev.slne.surf.nats.InternalNatsApi")
                }
            }
        }
    }
}
