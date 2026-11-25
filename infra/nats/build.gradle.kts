import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.util.slnePublic
import dev.slne.surf.api.gradle.util.slneReleases
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    id("dev.slne.surf.api.gradle.core") apply false
    alias(libs.plugins.weaver) apply false
}

subprojects {
    tasks.withType<ShadowJar>().configureEach {
        relocateNats()
        // Rewrites the BouncyCastle provider names in META-INF/services/java.security.Provider to
        // the relocated classes. Duplicates must reach the transformer instead of being dropped by
        // Shadow's EXCLUDE default.
        mergeServiceFiles()
        filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    }

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
                optIn.addAll(
                    "dev.slne.surf.nats.InternalNatsApi",
                    "dev.slne.surf.nats.NatsGeneratedApi",
                )
            }

            targets.withType<KotlinJvmTarget>().configureEach {
                compilerOptions {
                    freeCompilerArgs.add("-Xjsr305=strict")
                    javaParameters = true
                    optIn.add("dev.slne.surf.api.shared.api.util.InternalSurfApi")
                }
            }

            sourceSets {
                commonMain.dependencies {
                    compileOnly(kotlin("stdlib"))
                    compileOnly(libs.kotlinx.serialization.core)
                    compileOnly(libs.kotlinxCoroutines.core)
                }
                jsMain.dependencies {
                    api(kotlin("stdlib"))
                    api(libs.kotlinx.serialization.core)
                    api(libs.kotlinxCoroutines.core)
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

        val optInInternalApi = !project.path.startsWith(":surf-api-nats:surf-api-nats-test") &&
            !project.path.startsWith(":surf-api-nats:surf-api-nats-types")

        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                if (optInInternalApi) {
                    optIn.add("dev.slne.surf.nats.InternalNatsApi")
                }
            }
        }
    }
}
