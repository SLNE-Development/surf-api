@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

// Applied next to a module role, or in infra next to the surf Gradle plugin; both bring the
// conventions, so this plugin applies only the Kotlin plugin it configures.
plugins {
    kotlin("jvm")
}

extensions.configure<KotlinJvmProjectExtension> {
    abiValidation {
        filters {
            exclude {
                // May change in any release; what inlined plugin code references is
                // PublishedSurfApi.
                annotatedWith.add(SurfApiMarkers.INTERNAL)
            }
        }
    }
}

val checkInlineInternalApi = tasks.register<CheckInlineInternalApi>("checkInlineInternalApi") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description =
        "Fails when a public inline function references a declaration marked only @InternalSurfApi."
    val main = project.extensions.getByType<SourceSetContainer>().named("main")
    classes.from(main.map { it.output.classesDirs })
    classpath.from(main.map { it.compileClasspath })
}

tasks.named("check") { dependsOn(checkInlineInternalApi) }
