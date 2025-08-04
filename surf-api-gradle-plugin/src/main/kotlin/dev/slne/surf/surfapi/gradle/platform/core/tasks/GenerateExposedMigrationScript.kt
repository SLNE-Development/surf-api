package dev.slne.surf.surfapi.gradle.platform.core.tasks

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import java.util.*


val Project.sourceSets: SourceSetContainer get() = this.extensions.getByName("sourceSets") as SourceSetContainer
val SourceSetContainer.main: NamedDomainObjectProvider<SourceSet> get() = named<SourceSet>("main")

fun Project.generateExposedMigrationScript(cloudRuntimeDependency: String, mainClass: String) {
    val migrationRuntimeClasspath = configurations.create("migrationRuntimeClasspath") {
        extendsFrom(
            configurations["runtimeClasspath"].copyRecursive().apply { isCanBeResolved = false })
        isCanBeResolved = true
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    }

    dependencies {
        migrationRuntimeClasspath(cloudRuntimeDependency)
    }

    tasks.register<JavaExec>("generateExposedMigrationScript") {
        group = "migration"
        description = "Generate Exposed migration script"
        classpath = files(
            sourceSets.main.get().output,
            migrationRuntimeClasspath
        )
        this.mainClass.set(mainClass)

        val properties = project.file("migration.properties")

        doFirst {
            if (!properties.exists()) {
                properties.parentFile.mkdirs()
                properties.writeText(
                    """
                    # Migration database config
                    migration.dbUrl=jdbc:mysql://localhost:3306/database
                    migration.dbUser=
                    migration.dbPassword=
                    """.trimIndent()
                )
                throw GradleException("Created 'migration.properties' file. Please enter your credentials and run the task again.")
            }

            val migrationProperties = Properties().apply {
                load(properties.inputStream())
            }

            val requiredKeys = listOf("migration.dbUrl", "migration.dbUser", "migration.dbPassword")
            val missing =
                requiredKeys.filter { migrationProperties.getProperty(it).isNullOrBlank() }
            if (missing.isNotEmpty()) {
                throw GradleException("'migration.properties' is incomplete. Missing keys: ${missing.joinToString()}")
            }

            systemProperties(
                requiredKeys.associateWith { migrationProperties.getProperty(it) }
            )
        }
    }
}