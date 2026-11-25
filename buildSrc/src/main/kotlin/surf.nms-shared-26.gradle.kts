import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.util.*

/**
 * Convention for a 26.x NMS version module: compiles the shared 26.x sources into the module.
 *
 * `platform/paper/nms/shared-26/src/main/kotlin` holds the sources that every 26.x module shares.
 * They are written as the version that `shared-26/template.properties` names, the newest 26.x
 * module, so that module adds the directory as a Kotlin source directory as it is and the IDE
 * analyses the shared sources there.
 * Every older module copies them with `copyNmsTemplate` to `build/generated/nms-template/kotlin`,
 * with the package segment and class prefix of that version replaced by its own, derived from
 * its project name (`surf-api-paper-nms-v26-1` gives `v26_1` and `V26_1`).
 *
 * In an older module, a file under its own `src/main/kotlin` with the same path as a copied file
 * replaces the template file, so the module keeps only its deltas. The template module itself
 * cannot override a shared file; it changes the shared file instead.
 */

plugins {
    kotlin("jvm")
}

val sharedRoot = rootProject.layout.projectDirectory.dir("platform/paper/nms/shared-26")
val templateVersion: String = Properties()
    .apply { sharedRoot.file("template.properties").asFile.inputStream().use(::load) }
    .getProperty("version")
val templatePrefix = templateVersion.uppercase()
val versionPackage = project.name.substringAfterLast("-nms-").replace('-', '_')
val classPrefix = versionPackage.uppercase()

require(versionPackage.matches(Regex("v26_\\d+"))) {
    "${project.path} applies surf.nms-shared-26, but its name does not end in -nms-v26-<minor>"
}

val templateSources = sharedRoot.dir("src/main/kotlin")
val ownSources = layout.projectDirectory.dir("src/main/kotlin")

if (versionPackage == templateVersion) {
    val sharedFiles = templateSources.asFileTree.files
        .map { file -> file.relativeTo(templateSources.asFile).invariantSeparatorsPath }
    val overridden = sharedFiles.filter { path -> ownSources.file(path).asFile.exists() }

    check(overridden.isEmpty()) {
        "${project.path} is the template module and cannot override shared files: $overridden"
    }

    extensions.configure<KotlinJvmProjectExtension> {
        sourceSets.named("main") {
            kotlin.srcDir(templateSources)
        }
    }
} else {
    val copyNmsTemplate = tasks.register<Sync>("copyNmsTemplate") {
        group = "build"
        description = "Copies the shared 26.x NMS sources, minus the files this module overrides."

        from(templateSources)
        into(layout.buildDirectory.dir("generated/nms-template/kotlin"))
        inputs.dir(ownSources)
            .withPropertyName("ownSources")
            .optional()
        inputs.property("versionPackage", versionPackage)
        inputs.property("templateVersion", templateVersion)
        includeEmptyDirs = false

        val ownSourcesDir = ownSources.asFile
        eachFile {
            path = path.replace(templateVersion, versionPackage)
                .replace(templatePrefix, classPrefix)
            if (ownSourcesDir.resolve(path).exists()) {
                exclude()
            }
        }
        filter { line: String ->
            line.replace(templateVersion, versionPackage).replace(templatePrefix, classPrefix)
        }
    }

    extensions.configure<KotlinJvmProjectExtension> {
        sourceSets.named("main") {
            kotlin.srcDir(copyNmsTemplate)
        }
    }
}
