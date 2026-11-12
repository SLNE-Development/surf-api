import dev.slne.surf.api.generator.nms.NmsVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
    base
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24" apply false
    id("dev.slne.surf.api.generator.nms-module-generator")
}

nmsGenerator {
    referenceVersion = NmsVersion.V26_2
    targetVersion = NmsVersion.V26_3
}


allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            name = "minestom-snapshots"
            mavenContent {
                snapshotsOnly()
                includeGroup("net.minestom")
            }
        }
        maven("https://repo.wyck.dev/snapshots/")
    }

    configurations.all {
        if (name == "compileOnly") {
            return@all
        }

        dependencies.remove(project.dependencies.gradleApi())
        dependencies.remove(project.dependencies.gradleTestKit())
    }
}

val surfGroup = findProperty("group") as String
val surfVersion = findProperty("version") as String +
        if ((findProperty("snapshot") as String).toBooleanStrict()) "-SNAPSHOT" else ""
val modulePaths = subprojects.filter { it.subprojects.isEmpty() }.associate { it.name to it.path }

/**
 * Whether the project at [path] publishes its shaded jar as its `shadow` publication.
 *
 * Such a module's coordinate resolves to that jar outside this build, so code depending on it by
 * coordinate may import the packages it relocates.
 */
fun publishesShadedJar(path: String): Boolean {
    val publications = project(path).extensions.findByType<PublishingExtension>()?.publications
    return publications?.findByName("shadow") != null
}

allprojects {
    // The surf Gradle plugins and the infra builds request this build's modules by coordinate, as a
    // downstream project would (`dev.slne.surf.api:surf-api-paper:+`). Inside this build those
    // resolve to the projects instead of a published release, in the variant that release has.
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            val substitutions = this
            all {
                val requested = requested as? ModuleComponentSelector ?: return@all
                if (requested.group != surfGroup) return@all
                val path = modulePaths[requested.module] ?: return@all

                val target = substitutions.project(path)
                if (publishesShadedJar(path)) {
                    useTarget(substitutions.variant(target) {
                        attributes {
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
                        }
                    })
                } else {
                    useTarget(target)
                }
            }
        }
    }
}

// The Gradle plugin is an included build, which root lifecycle tasks do not reach on their own.
for (publishTask in listOf("publish", "publishToMavenLocal")) {
    tasks.register(publishTask) {
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        description = "Publishes the included build surf-api-gradle-plugin."
        dependsOn(gradle.includedBuild("gradle-plugin").task(":$publishTask"))
    }
}
tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":check"))
}

// The imported infra builds share this build's coordinates. Their modules do not apply
// core-convention, which sets them for every other module.
val infraDir = file("infra")
subprojects.filter { it.projectDir.startsWith(infraDir) }.forEach { infraProject ->
    infraProject.group = surfGroup
    infraProject.version = surfVersion

    // The infra modules register their ServiceLoader implementations with @AutoService, whose
    // processor the surf Gradle plugin stopped adding in 4.0.0.
    infraProject.pluginManager.withPlugin("com.google.devtools.ksp") {
        infraProject.dependencies {
            "compileOnly"(libs.auto.service.annotations)
            "ksp"(libs.auto.service.ksp)
        }
    }
}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinJvmExtension>()?.apply {
            compilerOptions {
                optIn.add("dev.slne.surf.api.shared.api.util.InternalSurfApi")
                freeCompilerArgs.add("-Xcontext-parameters")
            }
        }
    }
}